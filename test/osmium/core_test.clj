(ns osmium.core-test
  (:require [clojure.test :refer :all]
            [clj-webdriver.taxi :as taxi]
            [osmium.core :refer :all]))

(defmulti eval!* (fn [driver [type data]] type))

;; Navigation

(defmethod eval!* :refresh [driver _]
  (taxi/refresh driver))

(defmethod eval!* :back [driver _]
  (taxi/back driver))

(defmethod eval!* :forward [driver _]
  (taxi/forward driver))

(defmethod eval!* :to [driver [_ url]]
  (taxi/to driver url))

;; Page Actions

(defmethod eval!* :click [driver [_ sel]]
  (taxi/click driver sel))

(defmethod eval!* :fill [driver [_ {:keys [sel text]}]]
  (taxi/input-text driver sel text))

;; TODO: select options

(defmulti eval! (fn [driver xs] (type (first xs))))

(defmethod eval! clojure.lang.PersistentVector
  [driver xs]
  (doseq [x xs]
    (eval! driver x)))

(defmethod eval! clojure.lang.Keyword
  [driver x]
  (eval!* driver x))

(deftest a-test
  (testing "FIXME, I fail."
    (is (= 0 1))))
