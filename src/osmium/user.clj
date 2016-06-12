(ns osmium.user
  (:require [datomic.api :as d]))

(defn by-email [db email]
  (into {} (d/entity (d/db (:conn db)) [:user/email email])))

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
      {:user/email email :user/password password})))

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
