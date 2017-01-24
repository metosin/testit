(ns testit.eventually-test
  (:require [clojure.test :refer :all]
            [clojure.string :as str]
            [testit.core :refer :all]))

(deftest eventually-test
  (fact "immediatelly pass"
    (+ 1 2) =eventually=> 3
    3 =eventually=> (+ 1 2)
    (+ 1 2) =eventually=> pos?
    (+ 1 2) =eventually=> (partial > 2))

  (let [a (atom nil)]
    (future
      (Thread/sleep 100)
      (reset! a 1))
    (fact
      (deref a) =eventually=> 1))

  (fact "direct eventually tests"
    (eventually (partial = 1) (constantly 1)) => truthy
    (eventually (partial = 1) (constantly 2)) => falsey)

  (testing "failing eventually resolves in 1 sec"
    (let [start (System/currentTimeMillis)
          t (eventually (partial = 1) (constantly 2))
          end (System/currentTimeMillis)]
      (facts
        t => falsey
        (- end start) => (partial > 1060))))

  (testing "timeout can be changed to 0.5 sec"
    (binding [*eventually-timeout-ms* 500]
      (let [start (System/currentTimeMillis)
            t (eventually (partial = 1) (constantly 2))
            end (System/currentTimeMillis)]
        (facts
          t => falsey
          (- end start) => (partial > 560))))))

