(ns testit.facts-test
  (:require [clojure.test :refer [deftest]]
            [clojure.string :as str]
            [clojure.spec.alpha :as s]
            [testit.core :refer [any falsey truthy fact facts facts-for => =not=>]]))

(deftest basic-facts
  (facts
    42 => 42
    42 => pos?
    (+ 40 2) => 42

    [1 2 3] => [1 2 3]
    [1 2 3] => vector?

    42 =not=> 1337
    [1 2 3] =not=> string?)

  (let [answer 42
        half 21]
    (facts
      42 => pos?
      42 => (partial > 1337)
      42 => (* 2 21)
      42 => answer
      42 => (* half 2))))

(deftest some-regressions
  (let [a "a"]
    (fact
      a => a))

  (let [a "a"
        b "b"]
    (fact
      a =not=> b)))

(deftest facts-can-be-named
  (fact "42 is the answer"
    42 => integer?)
  (fact "vectors are not strings"
    [1 2 3] =not=> string?))

(deftest expected-can-be-fn-generating-fn
  (let [response {:status 200}
        status (fn [expected-status]
                 (fn [response]
                   (= (:status response) expected-status)))]
    (facts
      response => (status 200))))

(deftest facts-for-test
  (facts-for "multiple tests againts one value"
    42
    => integer?
    => pos?)
  (facts-for
    42
    => integer?
    => pos?
    =not=> string?
    => any
    => truthy
    =not=> falsey)
  (facts-for
    "foo"
    => string?
    => "foo"))
