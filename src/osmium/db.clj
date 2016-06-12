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
          user-schema (read "db/schemas/user.edn")
          entities (mapv #(assoc % :db/id (d/tempid :db.part/user))
                         (concat (read "db/seed/books.edn")
                                 (read "db/seed/users.edn")))
          db (d/create-database uri)
          conn (d/connect uri)]
      @(d/transact conn (concat book-schema user-schema))
      @(d/transact conn entities)
      (assoc component :conn conn)))
  (stop [component]
    (when conn (d/release conn))
    (assoc component :conn nil)))

(defn new-datomic-db
  ([] (new-datomic-db "datomic:mem://localhost:4334/osmium"))
  ([uri]
   (map->Datomic {:uri uri})))

(defn ->map [e]
  (when-let [id (:db/id e)]
    (into {:db/id id} e)))
