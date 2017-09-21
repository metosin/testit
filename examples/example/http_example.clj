(ns example.http-example
  (:require [clojure.test :refer :all]
            [testit.core :refer :all]
            [clojure.string :as str]
            [clj-http.client :as http]))

(deftest ^:slow test-google-response
  (fact
    (http/get "http://google.com")
    =in=>
    {:status 200
     :headers {"Content-Type" #(str/starts-with? % "text/html")}
     :body string?}))
