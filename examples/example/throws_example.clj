(ns example.throws-example
  (:require [clojure.test :refer :all]
            [clojure.string :as str]
            [testit.facts :refer :all]
            [testit.ex-info :refer [ex-info?]]
            [testit.contains :refer [contains]]))

(deftest =throws=>-examples
  (fact "Match exception class"
    (/ 1 0) =throws=> java.lang.ArithmeticException)

  (fact "Match exception class and message"
    (/ 1 0) =throws=> (java.lang.ArithmeticException. "Divide by zero"))

  (fact "Match against predicate"
    (/ 1 0) =throws=> #(-> % .getMessage (str/starts-with? "Divide")))

  (let [e (ex-info "oh no" {:reason "too lazy"})]
    (facts
      (throw e) =throws=> (ex-info? "oh no" {:reason "too lazy"})
      (throw e) =throws=> (ex-info? string? {:reason "too lazy"})
      (throw e) =throws=> (ex-info? string? (contains {:reason string?}))
      (throw e) =throws=> (ex-info? nil {:reason "too lazy"})
      (throw e) =throws=> (ex-info? "oh no" nil)
      (throw e) =throws=> (ex-info? nil nil))))
