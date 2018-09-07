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
      (f) =eventually=> 42))

  (let [a (atom nil)]
    (future
      (Thread/sleep 100)
      (reset! a 1))
    (fact
      (deref a) =eventually=> 1))

  (let [a (atom nil)]
    (future
      (Thread/sleep 1500)
      (reset! a 1))
    (fact
      {:timeout 2000}
      "wait for longer than the default timeout ms"
      (deref a) =eventually=> 1))

  (let [a (atom nil)]
    (future
      (Thread/sleep 100)
      (reset! a {:a "123"}))
    (fact
      (deref a) =eventually=> {:a #"\d+"})))

;
; Uncomment these to see failing tests:
;

#_(deftest ^:slow fail-with-wrong-actual-test
  (let [d (promise)]
    (deliver d -1)
    (fact {:timeout 100}
      (deref d) =eventually=> pos?)))
; =>
; Expected: (pos? (deref d))
; Actual: -1
; (deref d) =eventually= pos?

#_(deftest ^:slow fail-while-waiting-test
  (let [d (promise)]
    (fact {:timeout 100}
      (deref d) =eventually=> pos?)))
; =>
; Expected: (pos? (deref d))
; Actual: :testit.core/timeout
; (deref d) =eventually= pos?
