(ns testit.eventually-test
  (:require [clojure.test :refer :all]
            [clojure.string :as str]
            [testit.core :refer :all]))

(deftest ^:slow eventually-test
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

(deftest ^:slow eventually-in-test
  (fact "immediatelly pass"
    (+ 1 2) =eventually-in=> 3
    3 =eventually-in=> (+ 1 2)
    (+ 1 2) =eventually-in=> pos?
    (+ 1 2) =eventually-in=> (partial > 2))

  (let [a (atom nil)]
    (future
      (Thread/sleep 100)
      (reset! a 1))
    (fact
      (deref a) =eventually-in=> 1))

  (binding [*eventually-timeout-ms* 2000]
    (let [a (atom nil)]
      (future
        (Thread/sleep 1500)
        (reset! a 1))
      (fact "wait for longer than the default timeout ms"
        (deref a) =eventually-in=> 1)))

  (let [a (atom nil)]
    (future
      (Thread/sleep 100)
      (reset! a {:a "123"}))
    (fact
      (deref a) =eventually-in=> {:a #"\d+"})))
