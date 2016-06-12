(ns osmium.user
  (:require [datomic.api :as d]
            [osmium.db :as db]))

(defn by-id [db id]
  (db/->map (d/entity (d/db (:conn db)) id)))

(defn by-email [db email]
  (db/->map (d/entity (d/db (:conn db)) [:user/email email])))

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
      (new-user! db {:user/email email :user/password password})
      (by-email db email))))

(defn update-password! [db {:keys [email old-pass new-pass pass-confirm]}]
  (let [user (by-email db email)]
    (if-let [error (cond
                     (nil? user) :user-not-found
                     (not= (:user/password user) old-pass) :bad-pass
                     (not= new-pass pass-confirm) :pass-dont-match)]
      {:error error}
      (do
        (d/transact (:conn db) [{:user/email email :user/password new-pass}])
        (by-email db email)))))

(defn login [db {:keys [email password]}]
  (let [user (by-email db email)]
    (if-let [error (cond
                     (nil? user) :user-not-found
                     (empty? password) :empty-password
                     (not= (:user/password user) password) :pass-dont-match)]
      {:error error}
      user)))
