(ns osmium.eval
  (:require [clojure.string :as str]
            [clj-webdriver.taxi :as taxi]))

(declare eval!)

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

;; Helpers

(defn expand-action [driver [_ id]]
  (let [id-sel (str "#" id)]
    (when-let [action (taxi/attribute driver id-sel "data-action")]
      [(keyword action) id-sel])))

(defn- ele->id [ele]
  (taxi/attribute ele "id"))

(defn ele->action* [ele]
  [:action (ele->id ele)])

(defn ele->action [driver ele]
  (expand-action driver [:action (ele->id ele)]))

(defn find-actions [driver]
  (->> (taxi/find-elements driver {:css ".osmium-action"})
       (map ele->action*)
       set))

(defn possible-actions [driver]
  (conj (find-actions driver) :back :forward :back))

;; Page Actions

(defmethod eval!* :action [driver [_ id]]
  (when-let [action (expand-action driver [:action id])]
    (eval!* driver action)))

(defmethod eval!* :click [driver [_ sel]]
  (taxi/click driver sel))

(defmethod eval!* :input [driver [_ sel text]]
  (taxi/input-text driver sel (str text)))

(defmethod eval!* :fill [driver [_ mappings entity]]
  (eval! driver (->> mappings
                     (map (fn [[k q]]
                            (when-let [v (get entity k)]
                              [:input q v])))
                     (remove nil?)
                     vec)))

(defn reset-form! [driver id]
  (taxi/execute-script driver (format "document.getElementById('%s').reset();" id)))

;; Meant for :form
;; XXX: assumes that the the last html element submits the whole form
;; XXX: assumes that the selector is #id
(defmethod eval!* :submit [driver [_ sel]]
  (let [action-children (taxi/find-elements driver {:css (str sel " .osmium-action")})
        actions (mapv (partial ele->action driver) action-children)]
    (reset-form! driver (str/replace sel #"#" ""))
    (eval! driver actions)))

;; Spec

(comment
  (defn- spec-keyword? [k]
    (contains? (s/registry) k))
  (defmethod eval!* :default [driver [k sel]]
    (assert (spec-keyword? k) (str k " is not a spec keyword"))
    (let [val (gen/generate (s/gen k))]
      (eval!* driver [:input sel (str val)]))))

;; TODO: select options
;; TODO: wait (not necessary for a performance demo!)

(defmethod eval!* :wait [driver [_ milliseconds]]
  (Thread/sleep milliseconds))

(defmulti eval! (fn [driver xs] (type (first xs))))

(defmethod eval! nil [_ _])

(defmethod eval! clojure.lang.PersistentVector
  [driver xs]
  (doseq [x xs]
    (eval! driver x)))

(defonce last-exception (atom nil))

(defmethod eval! clojure.lang.Keyword
  [driver x]
  (try
    (eval!* driver x)
    (catch org.openqa.selenium.NoSuchElementException e
      (reset! last-exception e)
      (throw (Exception. (str "Couldn't find element for:" (pr-str x)))))))

(defn walk-n-steps!
  "Finds the possible actions for the driver's current state, randomly picks one and evals it,
  repeating the process n times, and calling (f action) at each step."
  ([d n] (walk-n-steps! d n (fn [_] nil)))
  ([d n f]
   (loop [n n
          actions []]
     (if (zero? n)
       actions
       (if-let [action (rand-nth (seq (find-actions d)))]
         (let [action' (expand-action d action)]
           (eval! d action')
           (if (= ::stop (f action'))
             (conj actions action')
             (recur (dec n) (conj actions action'))))
         actions)))))
