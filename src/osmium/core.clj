(ns osmium.core
  (:gen-class)
  (:require [clojure.string :as str]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [com.stuartsierra.component :as component]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring.middleware.params :as params]
            [ring.middleware.edn :refer [wrap-edn-params]]
            [ring.util.response :as response]
            [ring.adapter.jetty :as jetty]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [prone.middleware :as prone]
            [osmium.db :as db]
            [osmium.book :as book]
            [osmium.index :as index]))

;;  ======================================================================
;; Routes

(defn main-routes [db]
  (routes
   (GET "/" req (let [books (book/all db)]
                  (index/index books)))
   (GET "/book/:id" [id] (let [book (book/by-id db (Long. id))]
                           (index/book-view book)))
   (route/resources "/")))

;; ======================================================================
;; Server

(defn app-handler [db]
  (-> (main-routes db)
      wrap-edn-params
      wrap-reload
      prone/wrap-exceptions))

(defn start-jetty [handler port]
  (jetty/run-jetty handler {:port (Integer. port) :join? false}))

(defrecord Server [db port jetty]
  component/Lifecycle
  (start [component]
    (println "Start server at port " port)
    (assoc component :jetty (start-jetty (app-handler db) port)))
  (stop [component]
    (println "Stop server")
    (when jetty
      (.stop jetty))
    component))

(defn new-system [{:keys [http-port db-uri]}]
  (let [http-port (or http-port 3005)
        uri (or db-uri "datomic:mem://localhost:4334/osmium")]
    (component/system-map
     :db (db/map->Datomic {:uri uri})
     :server (component/using (map->Server {:port http-port}) [:db]))))

(defonce system (atom (new-system {})))

(defn stop! []
  (swap! system component/stop))

(defn start! []
  (swap! system component/start))

(defn restart! []
  (stop!)
  (start!))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Starting system")
  (start!))
