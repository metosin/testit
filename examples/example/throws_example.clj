(ns example.throws-example
  (:require [clojure.test :refer :all]
            [clojure.string :as str]
            [testit.facts :refer :all]))

(deftest =throws=>-examples
  (fact "Match exception class"
    (/ 1 0) =throws=> java.lang.ArithmeticException)

  (fact "Match exception class and message"
    (/ 1 0) =throws=> (java.lang.ArithmeticException. "Divide by zero"))

  (fact "Match against predicate"
    (/ 1 0) =throws=> #(-> % .getMessage (str/starts-with? "Divide")))

  (fact "Match ex-info exceptions"
    (throw (ex-info "oh no" {:reason "too lazy"}))
    =throws=>
    (ex-info? "oh no" any))

  (fact "Match ex-info exceptions with combine"
    (throw (ex-info "oh no" {:reason "too lazy"}))
    =throws=>
    (ex-info? any (contains {:reason string?})))


  (let [e (ex-info "oh no" {:reason "too lazy"})]
    (facts
      (throw e) =throws=> (ex-info? "oh no" {:reason "too lazy"})
      (throw e) =throws=> (ex-info? string? {:reason "too lazy"})
      (throw e) =throws=> (ex-info? string? (contains {:reason string?}))
      (throw e) =throws=> (ex-info? any {:reason "too lazy"})
      (throw e) =throws=> (ex-info? "oh no" any)
      (throw e) =throws=> (ex-info? any any))))
