(ns osmium.sauce-labs
  (:require [clj-webdriver.core]
            [clj-webdriver.remote-driver]
            [clj-webdriver.driver :as clj-driver]
            [clojure.edn :as edn]
            [clojure.java.io :as io])
  (:import [org.openqa.selenium.remote RemoteWebDriver]))

(def secrets (edn/read-string (slurp (io/resource "config/secrets.edn"))))

(defn new-sauce-labs-browser [username key]
  (let [url      (format "http://%s:%s@ondemand.saucelabs.com:80/wd/hub" username key)
        caps     (org.openqa.selenium.remote.DesiredCapabilities/firefox)
        webdriver   (org.openqa.selenium.remote.RemoteWebDriver. (java.net.URL. url) caps)]
    (clj-driver/init-driver webdriver)))
