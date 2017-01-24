(ns example.arrow-examples
  (:require [clojure.test :refer :all]
            [testit.core :refer :all]))

(deftest simple-form
  (facts
    (* 21 2) => 42))

(deftest function-predicate
  (facts
    (* 21 2) => integer?))

(deftest generate-predicate
  (facts
    (* 21 2) => (partial > 1337)))

(defn close-enough [expected-value margin]
  (fn [result]
    (<= (- expected-value margin) result (+ expected-value margin))))

(deftest function-generating-predicate
  (facts
    (* 21 2) => (close-enough 40 5)))
