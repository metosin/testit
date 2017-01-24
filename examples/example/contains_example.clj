(ns example.contains-example
  (:require [clojure.test :refer :all]
            [clojure.string :as str]
            [clj-http.client :as http]
            [testit.core :refer :all]))

(deftest basics-test
  (fact
    {:a 1 :b 2} => (contains {:a 1}))

  (fact
    {:a 1 :b 2} => (contains {:a pos?}))

  (fact
    {:a 1
     :b {:c 42
         :d {:e "foo"}}}
    => (contains {:b {:c pos?
                      :d {:e string?}}}))

  (fact
    [1 2 3] => (contains [1 pos? integer?]))

  (fact
    [{:a 1, :b 1}
     {:a 2, :b 2}
     {:a 3, :b 3}]
    => (contains [{:a 1}, map?, {:b pos?}]))

  (facts
    [1 2 3] => (contains [1 2 3 ...])
    [1 2 3 4] => (contains [1 2 3 ...])
    [1 2 3 4 5] => (contains [1 2 3 ...])

    [1 2] =not=> (contains [1 2 3 ...]))

  (facts
    [-1 0 +1] => (contains #{0 +1})
    [-1 0 +1] => (contains #{pos? neg?}))
  )



(deftest test-google-response
  (fact
    (http/get "http://google.com") =>
    (contains {:status 200
               :headers {"Content-Type" #(str/starts-with? % "text/html")}
               :body string?})))
