(ns testit.facts-test
  (:require [clojure.test :refer :all]
            [clojure.string :as str]
            [testit.core :refer :all]))

(deftest basic-facts
  (let [answer 42
        half 21]
    (facts
      42 => pos?
      42 => (partial > 1337)
      42 => (* 2 21)
      42 => answer
      42 => (* half 2))))

(deftest facts-can-be-named
  (fact "42 is the answer"
    42 => integer?))

(deftest expected-can-be-fn-generating-fn
  (let [response {:status 200}
        status (fn [expected-status]
                 (fn [response]
                   (= (:status response) expected-status)))]
    (facts
      response => (status 200))))

(deftest test-exceptions
  (fact "Match exception class"
    (/ 1 0) => java.lang.ArithmeticException)
  (fact "Match exception class and message"
    (/ 1 0) => (java.lang.ArithmeticException. "Divide by zero"))
  (fact "Match against predicate"
    (/ 1 0) => #(-> % .getMessage (str/starts-with? "Divide")))
  (facts
    (throw (ex-info "oh no" {:reason "too lazy"}))
    => (ex-info "oh no" {:reason "too lazy"}))
  (let [ei (ex-info "oh no" {:reason "too lazy"})]
    (facts "Special helper for ex-info"
      (throw ei) => (throws-ex-info "oh no" {:reason "too lazy"})
      (throw ei) => (throws-ex-info string? {:reason "too lazy"})
      (throw ei) => (throws-ex-info string? {:reason string?})
      (throw ei) => (throws-ex-info any {:reason "too lazy"})
      (throw ei) => (throws-ex-info "oh no"))))

;; FIXME: these tests are expected to fail, uncomment
;; and test to see failing some cases:
#_(deftest test-exception-failures
    (let [ei (ex-info "oh no" {:reason "too lazy"})]
      (fact "Wrong message"
        (throw ei) => (throws-ex-info "oh noz" {:reason "too lazy"}))
      (fact "Data does not match"
        (throw ei) => (throws-ex-info "oh no" {:reason "too lazyz"}))))

(deftest facts-for-test
  (facts-for "multiple tests againts one value" 42
    => integer?
    => pos?)
  (facts-for 42
    => integer?
    => pos?
    => any
    => truthy)
  (facts-for "foo"
    => string?
    => "foo"))
