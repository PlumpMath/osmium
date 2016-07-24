(ns osmium.core-test
  (:require [com.stuartsierra.component :as component]
            [clojure.string :as str]
            [clojure.test :refer :all]
            [clj-webdriver.taxi :as taxi]
            [osmium.core :as o]
            [osmium.eval :as eval]
            [osmium.sauce-labs :as sl]
            [osmium.user :as user]
            [osmium.web :as web]
            [clojure.spec :as s]
            [clojure.spec.gen :as gen]
            [clojure.set :as set]
            [clojure.test.check.generators :as test-gen ]
            [clj-webdriver.core :as wd]
            [clojure.string :as str]))

;; ======================================================================
;; API

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
         (eval/eval! driver path)
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

(defn make-tree*
  ([depth driver-fn]
   (make-tree* depth driver-fn [[:to "localhost:3005"]]))
  ([depth driver-fn path]
   (make-tree* depth driver-fn path (first path)))
  ([depth driver-fn path action]
   ;; base case returns whatever root action was passed in
   (if (zero? depth)
     action
     ;; start the system from scr
     (let [driver (driver-fn)]
       (try
         ;; reproduce state up to here
         (eval/eval! driver path)
         ;; find next level of actions
         (let [actions (mapv (partial expand-action driver) (find-actions driver))]
           ;; stop the system and recur along all actions
           (taxi/quit driver)
           (into [action]
                 (pmap #(make-tree* (dec depth) driver-fn (conj path %) %) actions)))
         (finally
           (taxi/quit driver)))))))

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
      (eval/eval! driver [:to "localhost:3005"])
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
    (eval/eval! @replay-driver next-action)
    (if (< step (dec (count actions)))
      (swap! replay update :step inc)
      (restart!))
    next-action))

;; ======================================================================
;; Useful Snippets

(comment
  (def d (taxi/new-driver {:browser :firefox}))

  (def d (sl/new-sauce-labs-browser))

  ;; start the system
  (o/start!)

  (eval/eval! d [:to "localhost:3005"])

  (def actions (walk-n-steps! d 10 identity))

  (def actions-for-recording (cons [:wait 1000] (interleave  actions (repeat [:wait 1000]))))

  ;; clean the system after walk-n-steps!
  (o/restart!)

  ;; replay the actions from the random walk, waiting 1sec between each one
  (eval/eval! d [:to "localhost:3005"])

  (eval/eval! d actions-for-recording)

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
