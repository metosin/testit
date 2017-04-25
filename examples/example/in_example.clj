(ns example.in-example
  (:require [clojure.test :refer :all]
            [clojure.string :as str]
            [clj-http.client :as http]
            [testit.core :refer :all]))

(deftest basics-test
  (fact
    {:a 1 :b 2} =in=> {:a 1})

  (fact
    {:a 1 :b 2} =in=> {:a pos?})

  (fact
    {:a 1
     :b {:c 42
         :d {:e "foo"}}}
    =in=>
    {:b {:c pos?
         :d {:e string?}}})

  (fact
    [1 2 3] =in=> [1 pos? integer?])

  (fact
    [{:a 1, :b 1}
     {:a 2, :b 2}
     {:a 3, :b 3}]
    =in=>
    [{:a 1}, map?, {:b pos?}])

  (facts
    [1 2 3] =in=> [1 2 3 ...]
    [1 2 3 4] =in=> [1 2 3 ...]
    [1 2 3 4 5] =in=> [1 2 3 ...])

  (facts
    [-1 0 +1] =in=> ^:in-any-order [0 +1]
    [-1 0 +1] =in=> ^:in-any-order [pos? neg?]))

(deftest test-google-response
  (fact
    (http/get "http://google.com")
    =in=>
    {:status 200
     :headers {"Content-Type" #(str/starts-with? % "text/html")}
     :body string?}))

; These fail (on purpose):

#_(deftest failing-test
  (fact
    {:a {:b {:c -1}}} =in=> {:a {:b {:c pos?}}})
  (fact
    [0 1 2 3] =in=> [0 1 42 3])
  (fact
    "foodar" =in=> "foobar")
  (fact
    {:a {:b [0 1 2 {:c "foodar"}]}}
    =in=>
    {:a {:b [0 neg? 2 {:c "foobar"}]}}))

