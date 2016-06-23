(ns osmium.core-test
  (:require [com.stuartsierra.component :as component]
            [clojure.string :as str]
            [clojure.test :refer :all]
            [clj-webdriver.taxi :as taxi]
            [osmium.core :as o]
            [osmium.user :as user]
            [osmium.web :as web]
            [clojure.spec :as s]
            [clojure.spec.gen :as gen]
            [clojure.set :as set]
            [clojure.test.check.generators :as test-gen ]
            [clj-webdriver.core :as wd]
            [clojure.string :as str]))

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

(defmethod eval!* :fill [driver [_ sel text]]
  (let [text (or text (gen/generate (s/gen string?)))]
    (taxi/input-text driver sel text)))

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

(defn- spec-keyword? [k]
  (contains? (s/registry) k))

(defmethod eval!* :default [driver [k sel]]
  (assert (spec-keyword? k) (str k " is not a spec keyword"))
  (let [val (gen/generate (s/gen k))]
    (eval!* driver [:fill sel (str val)])))

;; TODO: select options
;; TODO: wait (not necessary for a performance demo!)

(defmethod eval!* :wait [driver [_ milliseconds]]
  (Thread/sleep milliseconds))

;; ======================================================================
;; API

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

(defn make-tree
  "Generates a tree [action1-depth1 [[action1-depth-2 tree] [action2-depth2 tree]]]
  by walking all the possible paths from root to leaf node. It restarts the system on each run,
  reproduces the state by evaling path, finds the next level of actions, and recurs on all
  of them.
  - depth: of the tree
  - system: system object to be restarted on each run
  - driver: selenium driver object to use
  - path all previous steps necessary to reproduce the state, (for root call = [])
  - action: root action, ex. [:to 'localhost:30005']"
  ([depth system driver]
   (make-tree depth system driver [[:to "localhost:3005"]]))
  ([depth system driver path]
   (make-tree depth system driver path (first path)))
  ([depth system driver path action]
   ;; base case returns whatever root action was passed in
   (if (zero? depth)
     action
     ;; start the system from scr
     (let [system' (component/start system)]
       (try
         ;; reproduce state up to here
         (eval! driver path)
         ;; find next level of actions
         (let [actions (mapv (partial expand-action driver) (find-actions driver))]
           ;; stop the system and recur along all actions
           (component/stop system')
           (into [action]
                 (mapv #(make-tree (dec depth) system driver (conj path %) %) actions)))
         (finally
           ;; if an exception is thrown and any Server object is left in started state,
           ;; the port is bind and we have no way to stop it, need to restart the repl
           (component/stop system')))))))

;; ======================================================================
;; Tests

(defonce replay-driver (atom (taxi/new-driver {:browser :firefox})))

(defn restart-driver! []
  (swap! replay-driver (fn [d]
                         (taxi/quit d)
                         (taxi/new-driver {:browser :firefox}))))

(defonce replay (atom {:actions [] :step 0}))

(defn error-page? [page-source]
  (or (boolean (re-find #"error-page" page-source))
      (boolean (re-find #"not-found" page-source))))

(defn login-button-rendered? [page-source]
  (boolean (re-find #"login" page-source)))

(defn xor [a b]
  (and (or a b)
       (not (and a b))))

(use-fixtures :each (fn [t]
                      (o/start!)
                      (t)
                      (o/stop!)))

(deftest users
  (testing "All created users are valid"
    (let [driver (taxi/new-driver {:browser :firefox})
          db (:db @o/system)
          sessions (-> @o/system :session-store .session-map)]
      (eval! driver [:to "localhost:3005"])
      (let [actions (walk-n-steps! driver 10
                                   (fn [a]
                                     ;; this fn is called after each action
                                     (let [session (first (vals @sessions))]
                                       (doseq [user (user/all-users db)]
                                         (is (s/valid? ::user/user user)))
                                       (let [source (taxi/page-source driver)
                                             error-page? (error-page? source)]
                                         (is (not error-page?))
                                         (is (xor (web/logged-in? session)
                                                  (login-button-rendered? source)))))))]
        (reset! replay {:actions actions :step 0}))
      (taxi/quit driver))))

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

;; ======================================================================
;; Useful Snippets

(comment
  (def d (taxi/new-driver {:browser :firefox}))

  ;; start the system
  (o/start!)

  (eval! d [:to "localhost:3005"])

  (def actions (walk-n-steps! d 10 identity))

  (def actions-for-recording (cons [:wait 1000] (interleave  actions (repeat [:wait 1000]))))

  ;; clean the system after walk-n-steps!
  (o/restart!)

  ;; replay the actions from the random walk, waiting 1sec between each one
  (eval! d [:to "localhost:3005"])

  (eval! d actions-for-recording)

  ;; create a tree
  (def tree (make-tree 3 (o/new-system nil) d))

  ;; Run the tests (currently failing)
  (run-tests)

  ;; The results from the test are stored in replay
  ;; After running the test, step! should go through the actions

  (o/start!)
  (step!)

  (:actions @replay)
)
