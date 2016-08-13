(ns osmium.query
  (:require [clj-webdriver.taxi :as taxi]))

(defprotocol IQuery
  (q [sel dom]))

(defn map-vals [f m]
  (into {} (map (fn [[k v]] [k (f v)]) m)))

(defn q-map [sel dom]
  (->> (map-vals #(q % dom) sel)
       (remove (fn [[k v]] (empty? v)))
       (into {})))

(extend-protocol IQuery
  nil
  (q [sel dom] nil)

  java.lang.String
  (q [sel dom]
    (->> (taxi/find-elements dom {:css sel})
         (remove (comp nil? :webelement))
         vec))

  clojure.lang.PersistentVector
  (q [sel dom]
    (let [[x xs] sel]
      (if (nil? xs)
        (->> (taxi/find-elements dom {:css x})
             (remove (comp nil? :webelement))
             vec)
        (->> (taxi/find-elements dom {:css x})
             (remove (comp nil? :webelement))
             (mapv (partial q xs))
             (remove empty?)
             vec))))

  clojure.lang.PersistentArrayMap
  (q [sel dom]
    (q-map sel dom))
  clojure.lang.PersistentHashMap
  (q [sel dom]
    (q-map sel dom))

  clojure.lang.Fn
  (q [f dom]
    (f dom)))
