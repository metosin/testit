(ns example.basics
  (:require [clojure.test :refer :all]
            [testit.core :refer :all]
            [clojure.string :as str]
            [clj-http.client :as http]))

(deftest basic-examples

  (fact "left side evaluates to value that os equal to the right"
    (* 21 2) => 42)

  (facts "multiple assertions in single form"
    (+ 1 2) => 3
    (* 21 2) => 42
    (+ 623 714) => 1337)

  )

(deftest extended-equality-examples

  (facts "extended equality: function"
    (* 21 2) => integer?
    (* 21 2) => (partial > 1337))

  (testing "extended equality: map"

    (fact "right side defines requirements that must be present"
      {:a 1, :b 2} => {:a 1})

    (fact "extended equality is tested recursively"
      {:a 1,
       :b {:c 2}
       :d 3
       :e {:f "hello"}}
      =>
      {:b {:c 2}
       :e {:f string?}}))

  (testing "extended equality: vector"

    (fact
      [-1 0 +1] => [-1 0 +1])

    (fact
      [-1 0 +1] => [neg? zero? pos?])

    (fact
      [1 2 3 4 5] => [1 2 3 ...])

    (fact
      [-1 0 +1] => (in-any-order [0 1]))

    (fact "extended equality is tested recursively and can mix tests"
      [{:a 1, :b 1} {:a 2, :b 2} {:a 3, :b {:c 3}}]
      =>
      [{:a 1} map? {:b {:c pos?}}])

    )

  (testing "extended equality: set"

    (fact "translates to `(contains? #{:a :b :c} :a)`"
      :a => #{:a :b :c})

    )

  (testing "extended equality: regular expression"

    (fact
      (str 42) => #"\d+")

    )

  (testing "extended equality: class"

    (fact
      (/ 1 2) => clojure.lang.Ratio)

    )

  (testing "extended equality: exception instance"

    (fact
      (+ 1 "foo") =throws=> (ClassCastException. "java.lang.String cannot be cast to java.lang.Number"))

    (fact
      (+ 1 "f00") =throws=> (exception ClassCastException))

    (fact
      (+ 1 "f00") =throws=> (exception ClassCastException #"String.*cast"))

    )

  (testing "extended equality: ex-info"

    (let [f (fn []
              (throw (ex-info "oh no" {:error 42})))]
      (fact
        (f) =throws=> (ex-info "oh no" {:error 42})))

    (let [f (fn []
              (throw (ex-info "oh no" {:error 42})))]
      (fact
        (f) =throws=> (throws-ex-info #"no" {:error integer?})))

    )

  (testing "extended equality: nil"

    (fact
      (seq []) => nil)

    )

  (testing "extended equality: object"

    (fact
      (java.net.InetAddress/getByName "localhost") => (java.net.InetAddress/getByName "127.0.0.1"))

    )

  (testing "escape pod?"

    (fact
      {:a 1 :b 2} => (just {:a 1 :b 2}))

    ))

(deftest ^:slow eventually-example

  (testing "If you add `:timeout`, test will be evaluated until it passes, or it timeouts"
    (let [a (atom -1)]
      (future
        (Thread/sleep 50)
        (reset! a 1))
      (fact {:timeout 200}
        @a => pos?)))

  (testing "You can also use `=eventually=>` arrow, the default timeout is 1sec"
    (let [a (atom -1)]
      (future
        (Thread/sleep 100)
        (reset! a 1))
      (fact
        @a =eventually=> pos?)))

  )

(deftest http-example
  (fact "Google responds with 200 and some HTML"
    (http/get "http://google.com")
    =>
    {:status 200
     :headers {"Content-Type" #(str/starts-with? % "text/html")}
     :body string?}))

; Uncomment to see failing tests:

#_
(deftest failing-test
  (fact
    {:a {:b {:c -1}}}
    => {:a {:b {:c pos?}}})

  (fact
    "foodar" => "foobar")

  (fact
    (+ 1 "foo") => truthy)

  (fact
    (+ 1 "foo") => ClassCastException)
  )
