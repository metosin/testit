(ns testit.deep-compare-test
  (:require [clojure.test :refer :all]
            [clojure.string :as str]
            [testit.core :refer :all]))

(def deep-compare' #'testit.in/deep-compare)

(defmacro deep-compare [expected actual]
  `(deep-compare' [] (quote ~expected) ~expected ~actual))

(deftest basics-tests
  (fact
    (deep-compare {:a 1} {:a 1}) => [{:path [:a]
                                      :type :pass
                                      :message "(= 1 1) => true"
                                      :expected 1
                                      :actual 1}])
  (fact
    (deep-compare {:a 1} {:a 2}) => [{:path [:a]
                                      :type :fail
                                      :message "(= 1 2) => false"
                                      :expected 1
                                      :actual 2}])
  (fact
    (deep-compare {:a {:b {:c 1, :d 2}}}
                  {:a {:b {:c 1, :d 2}}})
    => [{:path [:a :b :c]
         :type :pass
         :message "(= 1 1) => true"
         :expected 1
         :actual 1}
        {:path [:a :b :d]
         :type :pass
         :message "(= 2 2) => true"
         :expected 2
         :actual 2}])
  (fact
    (deep-compare {:a {:b {:c 1, :d 2}}}
                  {:a {:b {:c 1, :d 3}}})
    => [{:path [:a :b :c]
         :type :pass
         :message "(= 1 1) => true"
         :expected 1
         :actual 1}
        {:path [:a :b :d]
         :type :fail
         :message "(= 2 3) => false"
         :expected 2
         :actual 3}])
  (fact
    (deep-compare {:a {:b {:c 1, :d 2}}}
                  {:a {:b {:c 1}}})
    => [{:path [:a :b :c]
         :type :pass
         :message "(= 1 1) => true"
         :expected 1
         :actual 1}
        {:path [:a :b :d]
         :type :fail
         :message "actual is missing key :d"
         :expected 2
         :actual nil}])
  (fact
    (deep-compare {:a {:b {:c 1}}}
                  {:a {:b {:c 1, :d 2}}})
    => [{:path [:a :b :c]
         :type :pass
         :message "(= 1 1) => true"
         :expected 1
         :actual 1}]))

; Comment this out are run tests so you can observe the error messages
; and IDE integration etc.

#_
(deftest failing-example
  (let [resp {:status 200
              :body {:name {:first "Tvler"
                            :last "Durden"}
                     :address {:street "537 Paper Street"
                               :po "Bradford"
                               :zip "19808"}}
              :headers {"Content-Type" "application/edn"}}]
    (fact
      resp =in=> {:status 200
                  :body {:name {:first "Tyler"
                                :last "Durden"}
                         :address {:street (fn [v] (re-find #"Paper" v))
                                   :zip (fn [v] (re-matches #"\d+" v))}}
                  :headers {"Content-Type" "application/edn"
                            "E-Tag" "112233"}})))
