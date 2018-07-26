(ns testit.ext-eq-test
  (:require [clojure.test :refer :all]
            [testit.core :refer [...]]
            [testit.ext-eq :as e]))

(deftest accept?-test
  (testing "set equality"
    (is (= (e/accept? #{:a :b} #{:a :b} :a [])
           [{:path []
             :type :pass
             :message "(contains? #{:b :a} :a) => true"
             :expected #{:a :b}
             :actual :a}]))
    (is (= (e/accept? #{:a :b} #{:a :b} :c [])
           [{:path []
             :type :fail
             :message "(contains? #{:b :a} :c) => false"
             :expected #{:a :b}
             :actual :c}])))

  (testing "regex equality"
    (let [re #"\d+"]
      (is (= (e/accept? re re "123" [])
             [{:path []
               :type :pass
               :message "(re-find #\"\\d+\" \"123\") => \"123\""
               :expected re
               :actual "123"}]))
      (is (= (e/accept? re re "123a" [])
             [{:path []
               :type :pass
               :message "(re-find #\"\\d+\" \"123a\") => \"123\""
               :expected re
               :actual "123a"}]))
      (is (= (e/accept? re re 42 [])
             [{:path []
               :type :fail
               :message "(re-find #\"\\d+\" 42) => false"
               :expected re
               :actual 42}]))
      (is (= (e/accept? re re "a" [])
             [{:path []
               :type :fail
               :message "(re-find #\"\\d+\" \"a\") => nil"
               :expected re
               :actual "a"}]))))

  (testing "class equality"
    (is (= (e/accept? java.lang.String java.lang.String "foo" [])
           [{:path []
             :type :pass
             :message nil
             :expected java.lang.String
             :actual "foo"}]))
    (is (= (e/accept? java.lang.String java.lang.String 42 [])
           [{:path []
             :type :fail
             :message "expected instance of java.lang.String, but got java.lang.Long"
             :expected java.lang.String
             :actual 42}])))

  (testing "nil equality"
    (is (= (e/accept? nil nil nil [])
           [{:path []
             :type :pass
             :message "(nil? nil) => true"
             :expected nil
             :actual nil}]))
    (is (= (e/accept? nil nil 42 [])
           [{:path []
             :type :fail
             :message "(nil? 42) => false"
             :expected nil
             :actual 42}])))

  (testing "default (Object) equality"
    (is (= (e/accept? (* 2 21) '(* 2 21) 42 [])
           [{:path []
             :type :pass
             :message "(= (* 2 21) 42) => true"
             :expected '(* 2 21)
             :actual 42}]))
    (is (= (e/accept? (* 2 21) '(* 2 21) 1337 [])
           [{:path []
             :type :fail
             :message "(= (* 2 21) 1337) => false"
             :expected '(* 2 21)
             :actual 1337}]))))

(deftest accept?-predicate-fn-test
  (testing "predicate function equality"
    (is (= (e/accept? pos? 'pos? 42 [])
           [{:path []
             :type :pass
             :message "(pos? 42) => true"
             :expected 'pos?
             :actual 42}]))
    (is (= (e/accept? pos? 'pos? -1 [])
           [{:path []
             :type :fail
             :message "(pos? -1) => false"
             :expected 'pos?
             :actual -1}]))
    (is (= (e/accept? pos? 'pos? nil [])
           [{:path []
             :type :fail
             :message "(pos? nil) => exception: java.lang.NullPointerException (message=nil, data=nil)"
             :expected 'pos?
             :actual nil}]))
    (is (= (e/accept? (fn [_] (throw (ex-info "Oh no" {:bad "data"})))
                      'foo?
                      :anything
                      [])
           [{:path []
             :type :fail
             :message "(foo? :anything) => exception: clojure.lang.ExceptionInfo (message=\"Oh no\", data={:bad \"data\"})"
             :expected 'foo?
             :actual :anything}]))))

(deftest accept?-ex-eq-fn-test
  (let [f (fn [_]
            (e/multi-result-response
              [{:path [:f]
                :type :fail
                :message "foo"
                :expected 'bar
                :actual 'boz}]))]
    (testing "extended equality function"
      (is (= (e/accept? f 'f true [:a :b])
             [{:path [:a :b :f]
               :type :fail
               :message "foo"
               :expected 'bar
               :actual 'boz}])))))


(deftest accept?-vector-test
  (is (= (e/accept? [1 2 3] [1 2 3] [1 2 3] [])
         [{:path [0]
           :type :pass
           :message "(= 1 1) => true"
           :expected 1
           :actual 1}
          {:path [1]
           :type :pass
           :message "(= 2 2) => true"
           :expected 2
           :actual 2}
          {:path [2]
           :type :pass
           :message "(= 3 3) => true"
           :expected 3
           :actual 3}]))
  (is (= (e/accept? [1 2 3] [1 2 3] [1 2 3 4 5] [])
         [{:path [0]
           :type :pass
           :message "(= 1 1) => true"
           :expected 1
           :actual 1}
          {:path [1]
           :type :pass
           :message "(= 2 2) => true"
           :expected 2
           :actual 2}
          {:path [2]
           :type :pass
           :message "(= 3 3) => true"
           :expected 3
           :actual 3}
          {:path [3]
           :type :fail
           :message "did not expect more than 3 elements"
           :expected nil
           :actual 4}]))
  (is (= (e/accept? [1 2 3 ...] [1 2 3 ...] [1 2 3 4 5] [])
         [{:path [0]
           :type :pass
           :message "(= 1 1) => true"
           :expected 1
           :actual 1}
          {:path [1]
           :type :pass
           :message "(= 2 2) => true"
           :expected 2
           :actual 2}
          {:path [2]
           :type :pass
           :message "(= 3 3) => true"
           :expected 3
           :actual 3}]))
  (is (= (e/accept? [1 2 3 ...] [1 2 3 ...] nil [])
         [{:path []
           :type :fail
           :message "(sequential? nil) => false"
           :expected [1 2 3 ...]
           :actual nil}]))
  (is (= (e/accept? [1 [2 pos? ...] 3]
                    '[1 [2 pos? ...] 3]
                    [1 [2 42 1337] 3]
                    [])
         [{:path [0]
           :type :pass
           :message "(= 1 1) => true"
           :expected 1
           :actual 1}
          {:path [1 0]
           :type :pass
           :message "(= 2 2) => true"
           :expected 2
           :actual 2}
          {:path [1 1]
           :type :pass
           :message "(pos? 42) => true"
           :expected 'pos?
           :actual 42}
          {:path [2]
           :type :pass
           :message "(= 3 3) => true"
           :expected 3
           :actual 3}])))

(deftest accept?-map-test
  (let [expected-form {:a 1
                       :b 'pos?
                       :c {:d ['neg?]}}
        expected-value (eval expected-form)]
    (is (= (e/accept? expected-value
                      expected-form
                      {:a 1
                       :b 2
                       :c {:d [-1]}}
                      [])
           [{:path [:a]
             :type :pass
             :message "(= 1 1) => true"
             :expected 1
             :actual 1}
            {:path [:b]
             :type :pass
             :message "(pos? 2) => true"
             :expected 'pos?
             :actual 2}
            {:path [:c :d 0]
             :type :pass
             :message "(neg? -1) => true"
             :expected 'neg?
             :actual -1}]))))

(deftest accept?-throwable
  (let [expected-value (Exception. "math is hard")
        expected-form '(Exception. "math is hard")
        actual (ArithmeticException. "math is hard")
        r (e/accept? expected-value expected-form actual [])]
    (testing "correct exception"
      (is (= r [{:path [:message]
                 :type :pass
                 :actual "math is hard"
                 :expected "math is hard",
                 :message "(= \"math is hard\" \"math is hard\") => true",}])))))

