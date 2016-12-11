(ns testit.contains
  (:require [clojure.test :refer :all]))

(defn- get! [m k]
  (if (contains? m k)
    (get m k)
    (reduced false)))

(defn- deep-compare [actual expected]
  (reduce-kv (fn [_ expected-k expected-v]
               (let [actual-v (get! actual expected-k)]
                 (or (cond
                       ; they are equal?
                       (= expected-v actual-v)
                       true
                       ; both are maps, go deeper:
                       (and (map? expected-v)
                            (map? actual-v))
                       (deep-compare actual-v expected-v)
                       ; expected is fn?
                       (or (symbol? expected-v)
                           (fn? expected-v))
                       (expected-v actual-v)
                       ; none of the above, fail:
                       :else false)
                     (reduced false))))
             false
             expected))

(defn contains [expected]
  {:pre [(map? expected)]}
  (fn [actual]
    (deep-compare actual expected)))

