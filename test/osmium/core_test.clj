(ns osmium.core-test
  (:import [clojure.test.check.generators Generator])
  (:require [clojure.test :refer :all]
            [clj-webdriver.taxi :as taxi]
            [osmium.core :as o]
            [osmium.user :as user]
            [clojure.spec :as s]
            [clojure.spec.gen :as gen]
            [clojure.set :as set]
            [clojure.test.check.generators :as test-gen ]))


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

(defmethod eval!* :fill [driver [_ sel text]]
  (let [text (or text (gen/generate (s/gen string?)))]
    (taxi/input-text driver sel text)))

;; Spec

(defn- spec-keyword? [k]
  (contains? (s/registry) k))

(defmethod eval!* :default [driver [k sel]]
  (assert (spec-keyword? k) (str k " is not a spec keyword"))
  (let [val (gen/generate (s/gen k))]
    (eval!* driver [:fill sel (str val)])))

;; TODO: select options
;; TODO: wait (not necessary for a performance demo!)

(defn- ele->id [ele]
  (taxi/attribute ele "id"))

(defn find-actions [driver]
  (->> (taxi/find-elements driver {:css ".osmium-action"})
       (map (fn [ele] [:action (ele->id ele)]))
       set))

(defn possible-actions [driver]
  (conj (find-actions driver) :back :forward :back))

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
  "Generates an action and evals it until n actions or no possible actions,
  calling (f action) at each step."
  ([d n] (walk-n-steps! d n (fn [_] nil)))
  ([d n f]
   (loop [n n
          actions []]
     (if (zero? n)
       actions
       (if-let [action (rand-nth (seq (find-actions d)))]
         (let [action' (expand-action d action)]
           (eval! d action')
           (f action')
           (recur (dec n) (conj actions action')))
         actions)))))

;; ======================================================================
;; Tests

(defonce replay-driver (atom (taxi/new-driver {:browser :firefox})))

(defn restart-driver! []
  (swap! replay-driver (fn [d]
                         (taxi/quit d)
                         (taxi/new-driver {:browser :firefox}))))

(defonce replay (atom {:actions [] :step 0}))

(deftest users
  (testing "All created users are valid"
    (o/start!)
    (let [driver (taxi/new-driver {:browser :firefox})
          db (:db @o/system)]
      (eval! driver [:to "localhost:3005"])
      (let [actions (walk-n-steps! driver 10
                                   (fn [a]
                                     (println a)
                                     (doseq [user (user/all-users db)]
                                       (is (s/valid? ::user/user user)))))]
        (reset! replay {:actions actions :step 0}))
      (taxi/quit driver))
    (o/stop!)))

(defn restart! []
  (swap! replay assoc :stop 0))

(defn step! []
  (let [{:keys [actions step]} @replay
        next-action (nth actions step)]
    (eval! @replay-driver next-action)
    (if (< step (dec (count actions)))
      (swap! replay update :step inc)
      (restart!))
    next-action))
