(ns osmium.sauce-labs
  (:require [clj-webdriver.core]
            [clj-webdriver.remote-driver]
            [clj-webdriver.driver :as clj-driver]
            [clojure.edn :as edn]
            [clojure.java.io :as io])
  (:import [org.openqa.selenium.remote RemoteWebDriver]))

(def secrets (edn/read-string (slurp (io/resource "config/secrets.edn"))))

(defn new-sauce-labs-browser []
  ;; TODO: Change this to pg-eng, move key to secure location (out of source), and cycle
  (let [username (get-in secrets [:sauce-labs :username])
        key      (get-in secrets [:sauce-labs :key])
        url      (format "http://%s:%s@ondemand.saucelabs.com:80/wd/hub" username key)
        caps     (org.openqa.selenium.remote.DesiredCapabilities/firefox)
        webdriver   (org.openqa.selenium.remote.RemoteWebDriver. (java.net.URL. url) caps)]
    (clj-driver/init-driver webdriver)))
