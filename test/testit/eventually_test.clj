(ns testit.eventually-test
  (:require [clojure.test :refer :all]
            [clojure.string :as str]
            [testit.core :refer :all]))

(deftest ^:slow eventually-test
  (facts "immediatelly pass"
    (+ 1 2) =eventually=> 3
    3 =eventually=> (+ 1 2)
    (+ 1 2) =eventually=> pos?
    (+ 1 2) =eventually=> (partial > 2))

  (let [p (promise)]
    (future
      (Thread/sleep 100)
      (deliver p 1))
    (fact
      (deref p) =eventually=> 1))

  (let [c (atom 0)
        f (fn []
            (swap! c inc))]
    (fact
      (f) =eventually=> 10))

  (let [c (atom 0)
        f (fn []
            (if (< (swap! c inc) 10)
              (throw (ex-info "oh no" {}))
              42))]
    (fact
      (f) =eventually=> 42)))

(deftest ^:slow direct-eventually-test
  (facts "direct eventually tests"
    (eventually (partial = 1) (fn [] (future 1))) => truthy
    (eventually (partial = 1) (fn [] (future 2))) => falsey)

  (testing "failing eventually resolves in 1 sec"
    (let [start (System/currentTimeMillis)
          t (eventually (partial = 1) (fn [] (future 2)))
          end (System/currentTimeMillis)]
      (facts
        t => {:success? false, :value 2}
        (- end start) => (partial > 1060))))

  (testing "timeout can be changed to 0.5 sec"
    (binding [*eventually-timeout-ms* 500]
      (let [start (System/currentTimeMillis)
            t (eventually (partial = 1) (fn [] (future 2)))
            end (System/currentTimeMillis)]
        (facts
          t => {:success? false, :value 2}
          (- end start) => (partial > 560)))))

  (testing "eventually resolved even when actual does not"
    (binding [*eventually-timeout-ms* 100]
      (let [start (System/currentTimeMillis)
            p (promise)
            t (eventually (partial = 1) (fn [] (future (deref p))))
            end (System/currentTimeMillis)]
        (facts
          t => {:success? false, :value :testit.core/timeout}
          (- end start) => (partial > 160)))))

  (testing "eventually handles exceptions in actual"
    (binding [*eventually-timeout-ms* 100]
      (let [start (System/currentTimeMillis)
            t (eventually (partial = 1) (fn [] (future (throw (ex-info "oh no" {})))))
            end (System/currentTimeMillis)]
        (facts
          t =in=> {:success? false, :value (ex-info? "oh no" {})}
          (- end start) => (partial > 160)))))

  (testing "eventually handles exceptions in actual, and keeps trying"
    (let [start (System/currentTimeMillis)
          c (atom 0)
          t (eventually (partial = 1) (fn []
                                        (future
                                          (if (< (swap! c inc) 5)
                                            (throw (ex-info "oh no" {}))
                                            1))))
          end (System/currentTimeMillis)]
      (facts
        t => {:success? true, :value 1}
        (- end start) => (partial > 260))))

  (testing "can wait for exception too"
    (let [start (System/currentTimeMillis)
          c (atom 0)
          t (eventually (ex-info? "oh no" {}) (fn []
                                                (future
                                                  (if (< (swap! c inc) 5)
                                                    1
                                                    (throw (ex-info "oh no" {}))))))
          end (System/currentTimeMillis)]
      (facts
        t =in=> {:success? true, :value (ex-info? "oh no" {})}
        (- end start) => (partial > 260)))))

(deftest ^:slow eventually-in-test
  (facts "immediatelly pass"
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

;
; Uncomment these to see failing tests:
;

#_(deftest ^:slow fail-with-wrong-actual-test
  (let [d (promise)]
    (deliver d -1)
    (fact (deref d) =eventually=> pos?)))
; =>
; Expected: (pos? (deref d))
; Actual: -1
; (deref d) =eventually= pos?

#_(deftest ^:slow fail-while-waiting-test
  (let [d (promise)]
    (fact (deref d) =eventually=> pos?)))
; =>
; Expected: (pos? (deref d))
; Actual: :testit.core/timeout
; (deref d) =eventually= pos?
