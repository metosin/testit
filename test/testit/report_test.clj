(ns testit.report-test
  (:require [clojure.test :refer :all]
            [testit.core :refer :all]))


(deftest foo-test
  (let [data {:a 1
              :b {:c 2}
              :c [1 2 3]
              :d {:e [{:f 1} {:f 2}]}}]
    (fact
      data =in=> {:a 1})
    (fact
      data =in=> {:a 2})
    (fact
      data =in=> {:a pos?})
    (fact
      data =in=> {:a neg?})))
