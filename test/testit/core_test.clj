(ns testit.core-test
  (:require [clojure.test :refer :all]
            [testit.contains :refer :all]))

(deftest contains-test
  (testing "basics"
    (let [t (contains {:a 1})]
      (is (t {:a 1}))
      (is (t {:a 1, :b 2}))
      (is (false? (t {})))
      (is (false? (t {:a 2})))))
  (testing "expected can be a fn or symbol"
    (let [t (contains {:status integer?
                       :body (fn [body]
                               (and (string? body)
                                    (<= (count body) 5)))})]
      (is (true? (t {:status 200
                     :body "Hello"})))
      (is (false? (t {:status 200
                      :body "Hello, world!"})))))
  (testing "recursively"
    (let [t (contains {:status 200
                       :headers {"Content-Type" "text/plain"
                                 "Content-Length" integer?}})]
      (is (true? (t {:status 200
                     :headers {"Content-Type" "text/plain"
                               "Content-Length" 42
                               "Cache-Control" "no-store"}
                     :body "hello"})))
      (is (false? (t {:status 200
                      :headers {"Content-Type" "text/plain"
                                "Cache-Control" "no-store"}
                      :body "hello"}))))))
