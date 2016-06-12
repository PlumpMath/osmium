(ns osmium.db
  (:require [datomic.api :as d]
            [com.stuartsierra.component :as component]
            [clojure.edn :as edn]
            [clojure.java.io :as io]))

(defrecord Datomic [uri conn]
  component/Lifecycle
  (start [component]
    (let [read (comp read-string slurp io/resource)
          book-schema (read "db/schemas/book.edn")
          books (mapv #(assoc % :db/id #db/id[:db.part/user])
                      (read "db/seed/books.edn"))
          db (d/create-database uri)
          conn (d/connect uri)]
      (d/transact conn book-schema)
      (d/transact conn books)
      (assoc component :conn conn)))
  (stop [component]
    (when conn (d/release conn))
    (assoc component :conn nil)))

(defn new-datomic-db [uri]
  (map->Datomic {:uri uri}))
