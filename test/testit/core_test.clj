(ns testit.core-test
  (:require [clojure.test :refer :all]
            [testit.core :refer :all]
            [clojure.data :as data]))

(deftest diff-contains
  (is (= [nil nil {:a 1}] (data/diff (contains {:a 1}) {:a 1 :b 2}))
      "Value contains the same keys & values as expected and an additional key")
  (is (= [{:a 1} nil nil] (data/diff (contains {:a 1}) {:b 2}))
      "Value is missing a key in expected.")
  (is (= [{:a 1} {:a 2} nil] (data/diff (contains {:a 1}) {:a 2}))
      "Value has different value for a key in expected.")
  (is (= [nil nil {:a {:b 2}}] (data/diff (contains {:a (contains {:b 2})}) {:a {:b 2 :z 1} :x 1}))
      "Works recursively")
  (is (= [{:a {:z 2}} {:a {:z 1}} {:a {:b 2}}] (data/diff (contains {:a (contains {:b 2 :z 2})}) {:a {:b 2 :z 1} :x 1}))
      "A difference in sub-map")

  (is (= [nil nil #{1 2}] (data/diff (contains #{1 2}) #{1 2 3})))
  ; FIXME:
  #_(is (= [nil nil #{1 2}] (data/diff (contains #{(contains {:a 1}) (contains {:b 2})}) #{{:a 1 :z 1} {:b 2 :z 1}})))

  (is (= [nil nil [{:a 1} {:b 1}]] (data/diff [{:a 1} {:b 1}] [{:a 1} {:b 1}]))
      "Sequential collections can contain maps")
  (is (= [nil nil [{:a 1} {:b 1}]] (data/diff [(contains {:a 1}) (contains {:b 1})]
                                              [{:a 1 :z 1} {:b 1 :z 1}]))
      "Sequential collections can contain maps with rules")
  )

(deftest diff-in-any-order
  (is (= [nil nil #{3 1 2}] (data/diff (in-any-order [3 1 2]) [1 2 3]))
      "Sequential collections can be marked with in-any-order")

  (is (= [#{4} nil #{3 1 2}] (data/diff (in-any-order [3 1 2 4]) [1 2 3]))
      "In-any-order differences are sets")

  ; FIXME:
  #_(is (= [nil nil [{:a 1} {:b 2}]] (data/diff (in-any-order [(contains {:a 1}) (contains {:b 2})])
                                              [{:a 1 :z 1} {:b 2 :z 1}]))
      "In-any-order should work with contains"))

(deftest same-test
  (is (same (contains {:a 1}) {:a 1 :z 1}))
  #_
  (is (same (contains {:a 1}) {:a 2 :z 1})
      "Should fail with (instead {:a 2} missing {:a 1})")

  (is (same (contains #{1 2}) #{1 2 3 4}))
  #_
  (is (same (contains #{1 2 5}) #{1 2 3 4})
      "Should fail with (instead nil missing #{5})")

  #_
  (is (same [{:a 1} {:b 2}]
            [{:a 1 :z 1} {:b 2 :z 1}]))
  #_
  (is (same [(contains {:a 1}) (contains {:b 2})]
            [{:a 1 :z 1} {:b 3 :z 1}])
      "Should fail with (instead [nil {:b 3}] missing [nil {:b 2}])"))
