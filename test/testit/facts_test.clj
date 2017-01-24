(ns testit.facts-test
  (:require [clojure.test :refer :all]
            [clojure.string :as str]
            [testit.core :refer :all]))

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

(deftest test-exceptions
  (fact "Match exception class"
    (/ 1 0) =throws=> java.lang.ArithmeticException)
  (fact "Match exception class and message"
    (/ 1 0) =throws=> (java.lang.ArithmeticException. "Divide by zero"))
  (fact "Match against predicate"
    (/ 1 0) =throws=> #(-> % .getMessage (str/starts-with? "Divide")))
  (let [ei (ex-info "oh no" {:reason "too lazy"})]
    (facts "Special helper for ex-info"
      (throw ei) =throws=> (ex-info? "oh no" (contains {:reason "too lazy"}))
      (throw ei) =throws=> (ex-info? string? (contains {:reason "too lazy"}))
      (throw ei) =throws=> (ex-info? string? (contains {:reason string?}))
      (throw ei) =throws=> (ex-info? any (contains {:reason "too lazy"}))
      (throw ei) =throws=> (ex-info? "oh no" any)
      (throw ei) =throws=> {:reason string?})))

(deftest test-excption-causes
  (fact
    (->> (java.lang.ArithmeticException. "3")
         (java.lang.RuntimeException. "2")
         (java.util.concurrent.ExecutionException. "1")
         (throw))
    =throws=> [(Exception. "1")
               (Exception. "2")
               (Exception. "3")])
  (fact
    (throw (ex-info "1" {:n 1 :a 42})) =throws=> [(ex-info? any (contains {:n 1}))])
  (fact
    (->> (ex-info "1" {:n 1 :a 42})
         (ex-info "2" {:n 2 :a 42})
         (ex-info "3" {:n 3 :a 42})
         (throw))
    =throws=> [(ex-info? "3" (contains {:n 3}))
               (ex-info? any any)
               (ex-info? any (contains {:a 42}))]))

; deftest macro disrupts macroexpand-1 somehow, that's why these are
; evaluated in here:
(def expanded-form-with-name (macroexpand-1 '(fact "foo is a string"
                                               "foo" => string?)))
(def expanded-form-without-name (macroexpand-1 '(fact
                                                  "foo" => string?)))

(deftest fact-name-is-optional-test
  (is (= '(clojure.test/is
            (=> string? "foo")
            "foo is a string")
         expanded-form-with-name))
  (is (= '(clojure.test/is
            (=> string? "foo")
            "foo => string?")
         expanded-form-without-name)))

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
