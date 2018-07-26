(ns example.midje-example
  (:require [clojure.test :refer :all]
            [testit.core :refer :all]))

(deftest midje-impersonation
  (facts
    (+ 1 2) => 3
    {:a 1 :z 1} => {:a 1}))
