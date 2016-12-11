(ns testit.facts-test
  (:require [clojure.test :refer :all]
            [clojure.string :as str]
            [testit.facts :refer :all]
            [testit.contains :refer [contains]]))

(deftest basic-facts
  (facts
    42 => 42
    42 => pos?
    (+ 40 2) => 42

    [1 2 3] => [1 2 3]
    [1 2 3] => vector?

    42 =not=> 1337
    [1 2 3] =not=> string?))

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
    (/ 1 0) =throws=> (fn [e]
                        (str/starts-with? (.getMessage e) "Divide"))))

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
