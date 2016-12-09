(ns testit.facts-test
  (:require [clojure.test :refer :all]
            [testit.facts :refer :all]))

(defn close? [value margin]
  (fn [result]
    (< (- value margin)
       result
       (+ value margin))))

(deftest clj-test-facts

  (testing "basics"

    (fact "\"foo\" is string"
      "foo" => string?)

    ; Without name
    (fact
      "foo" => string?)

    (fact
      42 => 42)

    (fact
      (+ 40 2) => 42)

    (fact
      42 => pos?)

    (fact
      (conj [1 2] 3) => [1 2 3])

    (fact "function"
      (+ 1 1) => pos?)

    (fact "function generating test function"
      40 => (close? 42 5))

    ; TODO: It would be great if this would work too:
    ; (fact
    ;   40 => (+ 40 2)))

  (testing "=not=>"
    (fact
      (+ 1 1) =not=> 1)

    (fact
      (+ 1 1) =not=> neg?)

    (fact
      1337 =not=> (close? 42 5)))))
