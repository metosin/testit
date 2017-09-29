(ns testit.in-test
  (:require [clojure.test :refer :all]
            [testit.core :refer :all]
            [testit.in :as in]))

(defn my-pos? [v]
  (pos? v))

(defmacro deep [expected actual]
  `(in/deep-compare nil (quote ~expected) ~expected ~actual))

(deftest deep-compare-values-test
  (fact "pass"
    (deep 1 1) => [{:path nil
                    :type :pass
                    :expected 1
                    :actual 1
                    :message "(= 1 1) => true"}])
  (fact "fail"
    (deep 1 2) => [{:path nil
                    :type :fail
                    :expected 1
                    :actual 2
                    :message "(= 1 2) => false"}])
  (fact "predicate pass"
    (deep pos? 1) => [{:path nil
                       :type :pass
                       :expected 'pos?
                       :actual 1
                       :message "(pos? 1) => true"}])
  (fact "predicate fail"
    (deep pos? -1) => [{:path nil
                        :type :fail
                        :expected 'pos?
                        :actual -1
                        :message "(pos? -1) => false"}])
  (fact "my predicate pass"
    (deep my-pos? 1) => [{:path nil
                          :type :pass
                          :expected 'my-pos?
                          :actual 1
                          :message "(my-pos? 1) => true"}])
  (fact "anon predicate pass"
    (deep (fn [v] (pos? v)) 1) => [{:path nil
                                    :type :pass
                                    :expected '(fn [v] (pos? v))
                                    :actual 1
                                    :message "((fn [v] (pos? v)) 1) => true"}]))

(deftest deep-compare-sequentials-test
  (fact "both are empty"
    (deep [] []) => [])
  (fact "both are empty, seq"
    (deep [] ()) => [])
  (fact "actual is not a sequential"
    (deep [] 42) => [{:path nil
                      :type :fail
                      :message "expected sequential"
                      :expected []
                      :actual 42}])
  (fact "simple equally long seqs"
    (deep [1 2 3] [1 2 3]) =in=> [{:type :pass}
                                  {:type :pass}
                                  {:type :pass}])
  (fact "simple equally long seqs, with error"
    (deep [1 2 3] [1 4 3]) =in=> [{:type :pass}
                                  {:type :fail
                                   :expected 2
                                   :actual 4}
                                  {:type :pass}])
  (fact "expected more"
    (deep [1 2 3] [1 2]) =in=> [{:type :pass}
                                {:type :pass}
                                {:type :fail
                                 :expected 3
                                 :actual nil
                                 :message "expected more than 2 elements"}])
  (fact "expected less"
    (deep [1 2] [1 2 3]) =in=> [{:type :pass}
                                {:type :pass}
                                {:type :fail
                                 :expected nil
                                 :actual 3
                                 :message "did not expect more than 2 elements"}])
  (fact "and then some"
    (deep [1 2 ...] [1 2 3]) =in=> [{:type :pass}
                                    {:type :pass}])
  (fact "and then some with same lengths"
    (deep [1 2 ...] [1 2]) =in=> [{:type :pass}
                                  {:type :pass}])
  (fact "and then some, but too few elements"
    (deep [1 2 ...] [1]) =in=> [{:type :pass}
                                {:type :fail
                                 :expected 2
                                 :actual nil
                                 :message "expected more than 1 elements"}]))

(deftest deep-compare-maps-test
  (fact "empty maps"
    (deep {} {}) => [])
  (fact "simple equal maps"
    (deep {:a 1} {:a 1}) =in=> [{:type :pass}])
  (fact "value differs"
    (deep {:a 1 :b 2} {:a 1 :b 3}) =in=> ^:in-any-order [{:path [:a]
                                                          :type :pass
                                                          :expected 1
                                                          :actual 1}
                                                         {:path [:b]
                                                          :type :fail
                                                          :expected 2
                                                          :actual 3}]))

(deftest deep-compare-in-any-order
  (fact "empty"
    (deep ^:in-any-order [] []) =in=> [])
  (fact "all found"
    (deep ^:in-any-order [1 2 3] [3 1 2]) =in=> [{:path [0]
                                                  :type :pass
                                                  :expected 1
                                                  :actual 1
                                                  :message "1 matches 1"}
                                                 {:path [1]
                                                  :type :pass
                                                  :expected 2
                                                  :actual 2
                                                  :message "2 matches 2"}
                                                 {:path [2]
                                                  :type :pass
                                                  :expected 3
                                                  :actual 3
                                                  :message "3 matches 3"}])
  (fact "all found with predicates"
    (deep ^:in-any-order [pos? neg?] [-1 1]) =in=> [{:path [0]
                                                     :type :pass
                                                     :expected 'pos?
                                                     :actual 1
                                                     :message "1 matches pos?"}
                                                    {:path [1]
                                                     :type :pass
                                                     :expected 'neg?
                                                     :actual -1
                                                     :message "-1 matches neg?"}])
  (fact "value missing"
    (deep ^:in-any-order [pos? neg?] [1]) =in=> [{:path [0]
                                                  :type :pass
                                                  :expected 'pos?
                                                  :actual 1
                                                  :message "1 matches pos?"}
                                                 {:path [1]
                                                  :type :fail
                                                  :expected 'neg?
                                                  :actual nil
                                                  :message "nothing matches neg?"}]))

(deftest regexp-support
  (fact "match regexp"
    "123" =in=> #"\d+")
  (fact "match uses re-find"
    "foo 123 bar" =in=> #"\d+")
  (fact "but it can be bound to start end end"
    "123" =in=> #"^\d+$"))

(deftest simple-in-test
  (fact
    "foo" =in=> "foo")
  (fact
    "foo" =in=> string?)
  (fact
    "foo" =in=> java.lang.String))
