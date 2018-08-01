(ns testit.core-test
  (:require [clojure.test :refer :all]
            [testit.core :as c]))

(deftest any-test
  (is (= true (c/any nil))))

(deftest trythy-test
  (is (= true (c/truthy :foo)))
  (is (= false (c/truthy nil)))
  (is (= false (c/truthy false))))

(deftest falsey-test
  (is (= false (c/falsey :foo)))
  (is (= true (c/falsey nil)))
  (is (= true (c/falsey false))))

(deftest just-test
  (let [p (c/just "foo")]
    (is (= true (p "foo")))
    (is (= false (p "bar")))))

(deftest is-not-test
  (let [p (c/is-not "foo")]
    (is (= false (p "foo")))
    (is (= true (p "bar")))))

(deftest throws-test
  (let [p (c/throws java.lang.RuntimeException)]
    (is (= [{:type :pass
             :expected java.lang.RuntimeException
             :message nil
             :path []}]
           (->> (p (java.lang.ArithmeticException.))
                :results
                (map #(dissoc % :actual)))))
    (is (= [{:type :fail
             :expected java.lang.RuntimeException
             :message "expected instance of java.lang.RuntimeException, but got java.io.IOException"
             :path []}]
           (->> (p (java.io.IOException.))
                :results
                (map #(dissoc % :actual))))))
  (let [p (c/throws java.lang.RuntimeException "oh no")]
    (is (= [{:type :pass
             :expected java.lang.RuntimeException
             :message nil
             :path []}
            {:type :pass
             :expected "oh no"
             :message "(= \"oh no\" \"oh no\") => true"
             :path [:message]}]
           (->> (p (java.lang.ArithmeticException. "oh no"))
                :results
                (map #(dissoc % :actual)))))
    (is (= [{:type :pass
             :expected java.lang.RuntimeException
             :message nil
             :path []}
            {:type :fail
             :expected "oh no"
             :message "(= \"oh no\" \"my bad\") => false"
             :path [:message]}]
           (->> (p (java.lang.ArithmeticException. "my bad"))
                :results
                (map #(dissoc % :actual)))))
    (is (= [{:type :pass
             :expected java.lang.RuntimeException
             :message nil
             :path []}
            {:type :fail
             :expected "oh no"
             :message "(= \"oh no\" nil) => false"
             :path [:message]}]
           (->> (p (java.lang.ArithmeticException.))
                :results
                (map #(dissoc % :actual)))))))

(deftest throws-ex-info-test
  (is (= [{:type :pass
           :expected clojure.lang.ExceptionInfo
           :message nil
           :path []}
          {:type :pass
           :expected "oh no"
           :message "(= \"oh no\" \"oh no\") => true"
           :path [:message]}]
         (->> ((c/throws-ex-info "oh no") (ex-info "oh no" {}))
              :results
              (map #(dissoc % :actual)))))
  (is (= [{:type :pass
           :expected clojure.lang.ExceptionInfo
           :message nil
           :path []}
          {:type :fail
           :expected "oh no"
           :message "(= \"oh no\" \"my bad\") => false"
           :path [:message]}]
         (->> ((c/throws-ex-info "oh no") (ex-info "my bad" {}))
              :results
              (map #(dissoc % :actual)))))
  (is (= [{:type :pass
           :expected clojure.lang.ExceptionInfo
           :path []}
          {:type :pass
           :expected "oh no"
           :path [:message]}
          {:type :pass
           :expected string?
           :path [:data :foo]}]
         (->> ((c/throws-ex-info "oh no" {:foo string?}) (ex-info "oh no" {:foo "bar"}))
              :results
              (map #(dissoc % :actual :message)))))
  (is (= [{:type :pass
           :expected clojure.lang.ExceptionInfo
           :path []}
          {:type :pass
           :expected "oh no"
           :path [:message]}
          {:type :fail
           :expected string?
           :path [:data :foo]}]
         (->> ((c/throws-ex-info "oh no" {:foo string?}) (ex-info "oh no" {:foo 42}))
              :results
              (map #(dissoc % :actual :message))))))

(def opts (assoc c/default-opts :timeout 100))

(deftest run-test-async-test
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

(deftest in-any-order-tests
  (is (= [{:path [0], :type :pass, :message "(= 1 1) => true", :expected 1, :actual 1}
          {:path [1], :type :pass, :message "(= 2 2) => true", :expected 2, :actual 2}]
         (-> ((c/in-any-order [1 2]) [1 2])
             :results)))
  (is (= [{:path [0], :type :pass, :message "(= 1 1) => true", :expected 1, :actual 1}
          {:path [1], :type :pass, :message "(= 2 2) => true", :expected 2, :actual 2}]
         (-> ((c/in-any-order [1 2]) [0 1 1/2 2 3])
             :results)))
  (is (= [{:path [0], :type :pass, :message "(= 1 1) => true", :expected 1, :actual 1}
          {:type :fail
           :actual [0 1 1/2 3]
           :expected 2
           :message "expected not found in actual values"
           :path [1]}]
         (-> ((c/in-any-order [1 2]) [0 1 1/2 3])
             :results)))
  (is (= [{:type :fail
           :actual [0 1/2 2 3]
           :expected 1
           :message "expected not found in actual values"
           :path [0]}
          {:path [1], :type :pass, :message "(= 2 2) => true", :expected 2, :actual 2}]
         (-> ((c/in-any-order [1 2]) [0 1/2 2 3])
             :results)))
  (is (= [{:path [0], :type :pass, :expected pos?, :actual +1}
          {:path [1], :type :pass, :expected neg?, :actual -1}]
         (->> ((c/in-any-order [pos? neg?]) [-1 +1])
              :results
              (map #(dissoc % :message))))))
