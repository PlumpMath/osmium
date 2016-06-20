(ns osmium.user
  (:require [clojure.spec :as s]
            [datomic.api :as d]
            [osmium.db :as db]))

;; ======================================================================
;; Data Model

(def email-regex #"^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,63}$")

(s/def :db/id number?)
(s/def ::email (s/and string? (partial re-matches email-regex)))
(s/def ::password (s/and string? #(< 6 (count %))))

(s/def ::user (s/keys :req [:db/id ::email ::password]))

(defn by-id [db id]
  (db/->map (d/entity (d/db (:conn db)) id)))

(defn by-email [db email]
  (db/->map (d/entity (d/db (:conn db)) [::email email])))

(defn new-user! [db user]
  (d/transact (:conn db) [(assoc user :db/id #db/id[:db.part/user])]))

(defn signup!
  "Validates signup form and returns either an {:error :code} or the new user"
  [db {:keys [email password pass-confirm]}]
  (if-let [error (cond
                   (empty? email) :empty-email
                   (empty? password) :empty-password
                   (not= password pass-confirm) :pass-dont-match)]
    {:error error}
    (do
      (new-user! db {::email email ::password password})
      (by-email db email))))

(defn update-password! [db {:keys [email old-pass new-pass pass-confirm]}]
  (let [user (by-email db email)]
    (if-let [error (cond
                     (nil? user) ::user-not-found
                     (not= (::password user) old-pass) :bad-pass
                     (not= new-pass pass-confirm) :pass-dont-match)]
      {:error error}
      (do
        (d/transact (:conn db) [{::email email ::password new-pass}])
        (by-email db email)))))

(defn login [db {:keys [email password]}]
  (let [user (by-email db email)]
    (if-let [error (cond
                     (nil? user) ::user-not-found
                     (empty? password) :empty-password
                     (not= (::password user) password) :pass-dont-match)]
      {:error error}
      user)))
