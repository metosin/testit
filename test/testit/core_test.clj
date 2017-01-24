(ns testit.core-test
  (:require [clojure.test :refer :all]
            [testit.facts :refer :all]))

(deftest basic-contains-test
  (let [t (contains {:a 1})]
    (is (t {:a 1}))
    (is (t {:a 1, :b 2}))
    (is (false? (t {})))
    (is (false? (t {:a 2})))))

(deftest sym-and-fn-test
  (let [t (contains {:status integer?
                     :body (fn [body]
                             (and (string? body)
                                  (<= (count body) 5)))})]
    (is (true? (t {:status 200
                   :body "Hello"})))
    (is (false? (t {:status 200
                    :body "Hello, world!"})))))

(deftest recursive-contains-test
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
                    :body "hello"})))))

(deftest contains-vectors-test
  (is (true? ((contains [1 2 3]) [1 2 3])))
  (is (false? ((contains [1 2 3]) [3 2 1])))
  (is (false? ((contains [1 2 3]) [1 2])))
  (is (false? ((contains [1 2 3]) [1 2 3 4])))
  (is (true? ((contains [1 2 ...]) [1 2])))
  (is (true? ((contains [1 2 ...]) [1 2 3])))
  (is (true? ((contains [1 2 ...]) [1 2 3 4])))
  (is (true? ((contains {:foo [1 2 3]}) {:foo [1 2 3]})))
  (is (true? ((contains [pos? neg?]) [1 -2])))
  (is (true? ((contains {:foo [pos? neg?]}) {:foo [1 -2]})))
  (is (true? ((contains [{:a 1} (contains {:a 2})]) [{:a 1} {:a 2 :b 3}]))))

(deftest contains-set-test
  (is (true? ((contains #{1 2 3}) [1 2 3])))
  (is (true? ((contains #{1 2 3}) [1 2 3 4 5])))
  (is (false? ((contains #{0 1 2 3}) [1 2 3 4 5])))
  (is (true? ((contains #{pos? neg? zero?}) [1 -1 0])))
  (is (false? ((contains #{pos? neg? zero?}) [1 -1 -2]))))
