(ns testit.core-test
  (:require [clojure.test :refer :all]
            [testit.core :as c]))

(def opts (assoc c/default-opts :timeout 100))

(deftest run-test-eventually-test
  (is (c/all-pass? (c/run-test-async
                     opts
                     42
                     '(* 2 21)
                     (constantly 42))))
  (is (= "timeout" (-> (c/run-test-async
                         opts
                         42
                         '(* 2 21)
                         (constantly 1337))
                       first
                       :message)))
  ; TODO: Add more eventually tests
  )

(deftest opts-name-and-body-test
  (is (= [nil
          nil
          '(1 => 2)]
         (c/opts-name-and-body
           '(1 => 2))))
  (is (= [{:foo "bar"}
          nil
          '(1 => 2)]
         (c/opts-name-and-body
           '({:foo "bar"} 1 => 2))))
  (is (= [nil
          "foo"
          '(1 => 2)]
         (c/opts-name-and-body
           '("foo" 1 => 2))))
  (is (= [{:foo "bar"}
          "foo"
          '(1 => 2)]
         (c/opts-name-and-body
           '({:foo "bar"} "foo" 1 => 2))))
  (is (= [nil
          nil
          '({:foo "bar"} => 2)]
         (c/opts-name-and-body
           '({:foo "bar"} => 2))))
  (is (= [nil
          nil
          '("bar" => 2)]
         (c/opts-name-and-body
           '("bar" => 2)))))
