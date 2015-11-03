(ns testit.facts-test
  (:require [clojure.test :refer :all]
            [testit.facts :refer :all]))

(deftest clj-test-facts

  (testing "basics"

    (fact ; "2 => 2"
      2 => 2)

    (fact "value"
      (+ 2 2) => 4)

    (fact "function"
      (+ 1 1) => pos?))

  (testing "=not=>"
    (fact (+ 1 1) =not=> 1)
    (fact (+ 1 1) =not=> neg?))

  (testing "facts"

    (facts ;unnamed
      1 => 1
      1 => pos?)

    (facts "more"
      1 =not=> 0
      1 =not=> neg?)))
