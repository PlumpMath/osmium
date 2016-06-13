(ns osmium.core
  (:gen-class)
  (:use [medley.core])
  (:require [clojure.string :as str]
            [clojure.set :as set]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [com.stuartsierra.component :as component]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring.middleware.params :as params]
            [ring.middleware.edn :refer [wrap-edn-params]]
            [ring.middleware.session :as session]
            [ring.util.response :as response]
            [ring.adapter.jetty :as jetty]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [prone.middleware :as prone]
            [osmium.db :as db]
            [osmium.book :as book]
            [osmium.index :as index]
            [osmium.user :as user]))

;;  ======================================================================
;; Routes

(defn main-routes [db]
  (routes
   (GET "/" {session :session}
     (let [books (book/all db)]
       (index/index session books)))
   (GET "/user/:email" req
     (let [email (get-in req [:params :email])]
       (if-let [user (user/by-email db email)]
         (index/user-view (:session req) user)
         "user not found")))
   (GET "/login" {session :session}
     (if (index/logged-in? session)
       (response/redirect "/")
       (index/log-in session)))
   (POST "/login" {params :params}
     (let [user (user/login db (map-keys keyword params))]
       (if-let [error (:error user)]
         (pr-str {:error error})
         (-> (response/redirect "/")
             (assoc-in [:session :user] user)))))
   (GET "/logout" _
     (-> (response/redirect "/")
         (assoc :session {})))
   (GET "/signup" {session :session} (index/sign-up session))
   (POST "/signup" {params :params}
     (let [new-user (user/signup! db (map-keys keyword params))]
       (if-let [error (:error new-user)]
         (pr-str error)
         (-> (format "/user/%s" (:user/email new-user))
             response/redirect
             (assoc :session {:user new-user})))))
   (POST "/update-pass" {params :params}
     (let [new-user (user/update-password! db (map-keys keyword params))]
       (if-let [error (:error new-user)]
         (pr-str error)
         (response/redirect (format "/user/%s" (:user/email new-user))))))
   (GET "/book" {session :session}
     (if-not (index/logged-in? session)
       (response/status (response/response "Not authorized") 401)
       (index/new-book session)))
   (POST "/book" {params :params session :session}
     (if-not (index/logged-in? session)
       (response/status (response/response "Not authorized") 401)
       (let [new-book (book/new-book! db (map-keys (partial keyword "book") params))]
         (if-let [error (:error new-book)]
           (pr-str error)
           (response/redirect (format "/book/%s" (:db/id new-book)))))))
   (GET "/book/:id" {params :params session :session}
     (let [book (book/by-id db (Long. (:id params)))
           mode (get params "mode")]
       (index/book-view session book {:edit? (= "edit" mode)})))

   (POST "/book/:id/rate" {params :params session :session}
     (if-not (index/logged-in? session)
       (response/status (response/response "Not authorized") 401)
       (if-let [rating (:rating (map-keys keyword params))]
         (do
           (book/update-rating! db (Long. (:id params)) (Integer. rating))
           (response/redirect (format "/book/%s" (:id params)))))))
   (POST "/book/:id/description" {params :params session :session}
     (if-not (index/logged-in? session)
       (response/status (response/response "Not authorized") 401)
       (let [description (get params "book/description")]
         (book/update-description! db (Long. (:id params)) description)
         (response/redirect (format "/book/%s" (:id params))))))
   (route/resources "/")))

;; ======================================================================
;; Server

(defn app-handler [db]
  (-> (main-routes db)
      session/wrap-session
      wrap-edn-params
      params/wrap-params
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
