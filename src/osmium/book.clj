(ns osmium.book
  (:require [datomic.api :as d]
            [osmium.db :as db]))

(def book-keys [:db/id :book/iban :book/title :book/author :book/description])

(defn by-id [db id]
  (db/->map (d/entity (d/db (:conn db)) id)))

(defn by-iban [db iban]
  (db/->map (d/entity (d/db (:conn db)) [:book/iban iban])))

(defn all [db]
  (mapv (comp (partial by-id db) first)
        (d/q '[:find ?b :in $ :where [?b :book/iban _]] (d/db (:conn db)))))


;; ======================================================================
;; Write

(defn create-book! [db book]
  (d/transact (:conn db) [(assoc book :db/id #db/id[:db.part/user])]))

(defn update-description! [db id description]
  (d/transact (:conn db) [{:db/id id :book/description description}]))

(defn update-rating! [db id rating]
  (d/transact (:conn db) [{:db/id id :book/rating rating}]))

(defn new-book! [db book]
  (if-let [error (cond
                   (empty? (:book/title book)) :empty-title
                   (empty? (:book/author book)) :empty-author
                   (empty? (:book/iban book)) :empty-iban
                   (empty? (:book/description book)) :empty-description)]
    {:error error}
    (do
      (create-book! db book)
      (by-iban db (:book/iban book)))))
