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

(defn expand-action [driver [_ id]]
  (let [id-sel (str "#" id)]
    (when-let [action (taxi/attribute driver id-sel "data-action")]
      [(keyword action) id-sel])))

(defmethod eval!* :action [driver [_ id]]
  (when-let [action (expand-action driver [:action id])]
    (eval!* driver action)))

(defmethod eval!* :click [driver [_ sel]]
  (taxi/click driver sel))

(defmethod eval!* :fill [driver [_ {:keys [sel text]}]]
  (taxi/input-text driver sel text))

;; TODO: select options
;; TODO: wait (not necessary for a performance demo!)


(defn- ele->id [ele]
  (taxi/attribute ele "id"))

(defn find-actions [driver]
  (letfn [(ele->action [])])
  (->> (taxi/find-elements driver {:css ".osmium-action"})
       (map (fn [ele] [:action (ele->id ele)]))
       set))

;; ======================================================================
;; API

(defmulti eval! (fn [driver xs] (type (first xs))))

(defmethod eval! clojure.lang.PersistentVector
  [driver xs]
  (doseq [x xs]
    (eval! driver x)))

(defmethod eval! clojure.lang.Keyword
  [driver x]
  (eval!* driver x))

(defn walk-n-steps!
  "Generates an action and evals it until n actions or no possible actions"
  [d n]
  (loop [n n
         actions []]
    (if (zero? n)
      actions
      (if-let [action (first (find-actions d))]
        (do
          (eval! d action)
          (recur (dec n) (conj actions action)))
        actions))))
