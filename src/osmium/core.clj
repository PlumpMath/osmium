(ns osmium.core
  (:gen-class)
  (:require [clojure.string :as str]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [com.stuartsierra.component :as component]
            [ring.middleware.params :as params]
            [ring.middleware.edn :refer [wrap-edn-params]]
            [ring.util.response :as response]
            [ring.adapter.jetty :as jetty]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [osmium.index :as index]))

(defroutes main-routes
  (GET "/" req (index/index))
  (route/resources "/"))

;; ======================================================================
;; Server

(def app-handler
  (-> main-routes
      wrap-edn-params
      handler/site))

(defn start-jetty [handler port]
  (jetty/run-jetty handler {:port (Integer. port) :join? false}))

(defrecord Server [port jetty]
  component/Lifecycle
  (start [component]
    (println "Start server at port " port)
    (assoc component :jetty (start-jetty app-handler port)))
  (stop [component]
    (println "Stop server")
    (when jetty
      (.stop jetty))
    component))

(defn new-system [{:keys [port]}]
  (Server. (or port 3005) nil))

(defonce *system* (atom (new-system {})))

(defn stop! []
  (swap! *system* component/stop))

(defn start! []
  (swap! *system* component/start))

(defn reset! []
  (stop!)
  (start!))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Starting system")
  (start!))
