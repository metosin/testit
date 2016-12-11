(ns testit.midje-example
  (:require [clojure.test :refer :all]
            [testit.facts :refer :all]
            [testit.contains :refer [contains]]
            [clojure.string :as str]))

(deftest midje-impersonation
  (facts
    (+ 1 2) => 3
    {:a 1 :z 1} => (contains {:a integer?})))
