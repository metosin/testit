(ns example.contains-example
  (:require [clojure.test :refer :all]
            [clojure.string :as str]
            [clj-http.client :as http]
            [testit.facts :refer :all]))

(deftest basics-test
  (fact
    {:a 1 :b 2} => (contains {:a 1}))

  (fact
    {:a 1 :b 2} => (contains {:a pos?}))

  (fact
    {:a 1
     :b {:c 42
         :d {:e "foo"}}} => (contains {:b {:c pos?
                                           :d {:e string?}}})))

(deftest test-google-response
  (fact
    (http/get "http://google.com") =>
    (contains {:status 200
               :headers {"Content-Type" #(str/starts-with? % "text/html")}
               :body string?})))
