(ns example.midje-example
  (:require [clojure.test :refer :all]
            [testit.facts :refer :all]
            [testit.contains :refer [contains]]))

(deftest midje-impersonation
  (facts
    (+ 1 2) => 3
    {:a 1 :z 1} => (contains {:a 1})))
