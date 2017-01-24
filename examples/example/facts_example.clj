(ns example.facts-example
  (:require [clojure.test :refer :all]
            [testit.core :refer :all]))

(deftest group-multiple-assertions
  (facts "simple match tests"
    (+ 1 2) => 3
    (* 21 2) => 42
    (+ 623 714) => 1337))
