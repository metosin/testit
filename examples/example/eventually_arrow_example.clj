(ns example.eventually-arrow-example
  (require [clojure.test :refer :all]
           [testit.core :refer :all]))

(deftest eventually-example

  (testing "Left-side will match right side eventually"
    (let [a (atom -1)]
      (future
        (Thread/sleep 100)
        (reset! a 1))
      (fact
        (deref a) =eventually=> pos?)))

  (testing "You can change the timeout from it's default of 1sec"
    (binding [*eventually-timeout-ms* 2000]
      (let [a (atom -1)]
        (future
          (Thread/sleep 1500)
          (reset! a 1))
        (fact
          (deref a) =eventually=> pos?))))

  )