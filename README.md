# testit

(Midje)[https://github.com/marick/Midje] style assertions for `clojure.test`

Clojars dependency: `[metosin/testit "0.1.0-SNAPSHOT"]`

## Goals and non-goals

*Goals:*

* Allow writing tests in Midje style with `=>` and `=not=>`
* Use (and extends) `clojure.test`
* Allow extending

*Non-goals:*

* Does not provide 100% compatibility with Midje
* Does not improve output

## Quick intro for Midje users

with Midje:

```clj
(ns some.midje.tests 
  (:require [midje.sweer :refer :all]))

(facts
  (+ 1 2) => 3
  {:a 1 :z 1} => (contains {:a 1}))
```

with `testit.facts`:

Add dependency to `[metosin/testit "0.1.0-SNAPSHOT"]`

```clj
(ns example.midje-example
  (:require [clojure.test :refer :all]
            [testit.facts :refer :all]
            [testit.contains :refer [contains]]))

(deftest midje-impersonation
  (facts
    (+ 1 2) => 3
    {:a 1 :z 1} => (contains {:a 1})))
```

## Facts

The `fact` macro generates a `clojure.test/is` form with the arrow in 
place of test function.

```clj
(macroexpand-1 '(fact (+ 1 2) => 3))
=> (clojure.test/is (=> 3 (+ 1 2)) "(+ 1 2) => 3")
```

The `testit` extends basic `clojure.test/is` functionality
by adding appropriate assertion logic for `=>`, `=not=>` and `=throws=>` 
symbols.

The `facts` allows grouping of multiple assertions into a single form. For example:

```clj
(deftest group-multiple-assertions
  (facts "simple match tests"
    (+ 1 2) => 3
    (* 21 2) => 42
    (+ 623 714) => 1337))
```

## The `=>` and `=not=>` arrows

The left side of `=>` arrow is a form that is evaluated once. The right side can be
a simple form that is also evaluated once. Test is performed by comparing the evaluated
values for equality (or non-equality in case of `=not=>`).

```clj
(deftest simple-form
  (facts
    (* 21 2) => 42))
```

The right side of `=>` can also be a symbol to a function. In that case the test is 
performed by passing the value of the left side to the function.

```clj
(deftest function-predicate
  (facts
    (* 21 2) => integer?))
```

Finally, the right side can also be a list form with a function call. In that case, the 
test is performed against the evaluated function.
 
```clj
(defn close-enough [expected-value margin]
  (fn [result]
    (<= (- expected-value margin) result (+ expected-value margin))))

(deftest function-generating-predicate
  (facts
    (* 21 2) => (close-enough 40 5)))
```

## The `=throws=>` arrow

The `=throws=>` arrow can be used to assert that the evaluation of the left side
throws an exception. The right side of `=throws=>` can be:

# Class extending `java.lang.Throwable`
# On object impementing `java.lang.Throwable`
# A predicate function

If the right side is a class, the assertion is made to ensure that the left side
throws an exception that is, or extends, the class on the right side.

```clj
(fact "Match exception class"
  (/ 1 0) =throws=> java.lang.ArithmeticException)
```

Second case ensures that the exception type is, or extends, the type of the right
side and also that the message of the exception equals that of the message on right
side.

```clj
(fact "Match exception class and message"
  (/ 1 0) =throws=> (java.lang.ArithmeticException. "Divide by zero"))
```

Finally, you can provide your own function to perform your custom comparion.

```clj
(fact "Match against predicate"
  (/ 1 0) =throws=> #(-> % .getMessage (str/starts-with? "Divide")))
```

## `contains`

Very often you want to test that the left side evaluates to a map with
some required key/value pairs. To help writing this kind of tests the
`testit` provides a function `testit.contains/contains`. Here's a quic
example:

```clj
(fact
  {:a 1 :b 2} => (contains {:a 1}))
```

`contains` accepts a map with required keys and values, and returns a function
that asserts that the left side contains those key/value pairs.

The values of the map may also be predicates:

```clj
(fact
  {:a 1 :b 2} => (contains {:a pos?}))
```

Finally, the map can be of any depth:

```clj
(fact
  {:a 1
   :b {:c 42
       :d {:e "foo"}}} => (contains {:b {:c pos?
                                         :d {:e string?}}}))
```

A very common use-case for this kind of testing is testing the HTTP responses. Here's
an example.

```clj
(ns example.contains-example
  (:require [clojure.test :refer :all]
            [clojure.string :as str]
            [clj-http.client :as http]
            [testit.facts :refer :all]
            [testit.contains :refer [contains]]))

(deftest test-google-response
  (fact
    (http/get "http://google.com")
    => (contains {:status 200
                  :headers {"Content-Type" #(str/starts-with? % "text/html")}
                  :body string?})))
```

## Helper for `clojure.core/ex-info`

Using `clojure.core/ex-info` to generate an exception is quite common, so
`testit` includes an helper for that.

The `testit.ex-info/ex-info?` function acceps two parameters. First is user to
test the exception message, second the message data. Both can be values (tested
with `=), predicate functions, or nil (for ignoring the value).

```clj
(let [e (ex-info "oh no" {:reason "too lazy"})]
  (facts
    (throw e) =throws=> (ex-info? "oh no" {:reason "too lazy"}
    (throw e) =throws=> (ex-info? string? {:reason "too lazy"})
    (throw e) =throws=> (ex-info? string? (contains {:reason string?}))
    (throw e) =throws=> (ex-info? nil {:reason "too lazy"})
    (throw e) =throws=> (ex-info? "oh no" nil)
    (throw e) =throws=> (ex-info? nil nil)))
```

In the example above, all tests pass.

## Extending `testit` with your own arrows

You can add your custom arrows using `clojure.test/assert-expr`. For example,
here's a simple extension that asserts that the test is completed within 1 sec.

```clj
(ns testit.your-own-arrow
  (:require [clojure.test :refer :all]
            [testit.facts :refer :all]))

(declare =quickly=>)
(defmethod assert-expr '=quickly=> [msg [_ & body]]
  (assert-expr msg `(let [d# (future ~@body)
                          r# (deref d# 1000 ::timeout)]
                      (if (= r# ::timeout)
                        false
                        r#))))
```

And then you use it like this:

```clj
(deftest things-must-be-fast-tests
  (fact
    (do (Thread/sleep 200) 42) =quickly=> 42)
  (fact
    (do (Thread/sleep 2000) 42) =quickly=> 42))
```

In the example above, the first fact passes but the second fails after 1 sec.

## TODO

- [ ] Detect and complain about common mistakes
- [ ] Provide better error messages when right side is a predicate
- [ ] Devise way to support [humane-test-output](https://github.com/pjstadig/humane-test-output) style output

## License

Copyright © 2016 [Metosin Oy](http://metosin.fi)

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
