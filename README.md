# testit [![Build Status](https://api.travis-ci.org/metosin/testit.svg?branch=master)](https://travis-ci.org/metosin/testit)

[Midje](https://github.com/marick/Midje) style assertions for `clojure.test`

Clojars dependency: `[metosin/testit "0.0.1"]`

**Note**: This library is still under heavy development!

## Goals and non-goals

*Goals:*

* Allow writing tests in Midje style with `=>` and `=not=>`
* Uses (and extends) `clojure.test`
* Allow extending functionality

*Non-goals:*

* Does not provide any compatibility with Midje
* Does not improve output

## Quick intro for Midje users

with Midje:

```clj
(ns some.midje.tests 
  (:require [midje.sweet :refer :all]))

(facts
  (+ 1 2) => 3
  {:a 1 :z 1} => (contains {:a 1}))
```

with `testit`:

```clj
(ns example.midje-example
  (:require [clojure.test :refer :all]
            [testit.core :refer :all]))

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

The `facts` allows grouping of multiple assertions into a single form. For 
example:

```clj
(deftest group-multiple-assertions
  (facts "simple match tests"
    (+ 1 2) => 3
    (* 21 2) => 42
    (+ 623 714) => 1337))
```

## The `=>` and `=not=>` arrows

The left side of `=>` arrow is a form that is evaluated once. The right side 
can be a simple form that is also evaluated once. Test is performed by 
comparing the evaluated values for equality (or non-equality in case of 
`=not=>`).

```clj
(deftest simple-form
  (facts
    (* 21 2) => 42))
```

The right side of `=>` can also be a predicate function. In that case the 
test is performed by passing the value of the left side to the predicate.

```clj
(deftest function-predicate
  (facts
    (* 21 2) => integer?))
```

A common practice is to call function that returns a predicate. For example:

```clj
(deftest generate-predicate
  (facts
    (* 21 2) => (partial > 1337)))
```

A bit more complex example:
 
```clj
(defn close-enough [expected-value margin]
  (fn [result]
    (<= (- expected-value margin) result (+ expected-value margin))))

(deftest function-generating-predicate
  (facts
    (* 21 2) => (close-enough 40 5)))
```

## Testing exceptions with the `=throws=>` arrow

The `=throws=>` arrow can be used to assert that the evaluation of the left side
throws an exception. The right side of `=throws=>` can be:

* A class extending `java.lang.Throwable`
* An object extending `java.lang.Throwable`
* A predicate function
* A seq of the above

If the right side is a class, the assertion is made to ensure that the left side
throws an exception that is, or extends, the class on the right side.

```clj
(fact "Match exception class"
  (/ 1 0) =throws=> java.lang.ArithmeticException)
```

You can also use an exception object. This ensures that the exception is 
of correct type, and also that the message of the exception equals that of 
the message on right side.

```clj
(fact "Match exception class and message"
  (/ 1 0) =throws=> (java.lang.ArithmeticException. "Divide by zero"))
```

Most flexible case is to use a predicate function.

```clj
(fact "Match against predicate"
  (/ 1 0) =throws=> #(-> % .getMessage (str/starts-with? "Divide")))
```

Finally, `=throws=>` supports a sequence. The thrown exception is tested 
against the first element from the seq, and it's cause to the second, and 
so on. This is very handy when the actual exception you are interested is 
wrapped into another exception, for example to 
`java.util.concurrent.ExecutionException`.

This example creates an `java.lang.ArithmeticException`, wrapped into a 
`java.lang.RuntimeException`, wrapped into a 
`java.util.concurrent.ExecutionException`. 

The `fact` then tests that the left side throws an exception that extends 
`java.lang.Exception` and has a message `"1"`, and that is caused by another 
exception, also extending `java.lang.Exception` with message `"2"`, that is 
caused yet another exception, this time with message `"3"`:

```clj
(fact
  (->> (java.lang.ArithmeticException. "3")
       (java.lang.RuntimeException. "2")
       (java.util.concurrent.ExecutionException. "1")
       (throw))
  =throws=> [(Exception. "1")
             (Exception. "2")
             (Exception. "3")])
```

A common pattern with Clojure code is to generate exceptions with 
`clojure.core/ex-info`. To help testing these kind of exceptions, `testit` 
provides a function `testit.core/ex-info?`. The function accepts a message 
(string or a predicate) and a data (map or a predicate), and returns a predicate
that tests given exception type (must extend `clojure.lang.IExceptionInfo`), 
message and data.

```clj
(fact "Match ex-info exceptions"
  (throw (ex-info "oh no" {:reason "too lazy"}))
  =throws=>
  (ex-info? "oh no" {:reason "too lazy"}))
```

The above test ensures that the left side throws an `ex-info` exception with 
expected message and data.  

### Helper predicates

#### `any`

The `any` is a predicate that matches anything. It's implemented like
this:

```clj
; in ns testit.core:
(def any (constantly true))
```

#### `truthy` and `falsey`

Other helper predicate are `testit.core/truthy` and `testit.core/falsey` 
which test given values for clojure 'truthines' and 'falsines' respectively.

## The `=eventually=>` arrow

You can use the `=eventually=>` arrow to test async code.

```clj
(let [a (atom -1)]
  (future
    (Thread/sleep 100)
    (reset! a 1))
  (fact
    (deref a) =eventually=> pos?))
```

On the code above, the result of evaluating the `(deref a)` is initially -1. 
The test does not match the expected predicate `pos?`. How ever, the 
`=eventually=>` will keep repeating the test until the test matches, or a 
timeout occurs. Eventually the future resets the atom to 1 and the test
passes.

By default the `=eventually=>` keeps evaluating and testing every 50 ms and 
the timeout is 1 sec. You can change these by binding 
`testit.core/*eventually-polling-ms*` and 
`testit.core/*eventually-timeout-ms*`. For example, code below sets the 
timeout to 2 sec.

```clj
(testing "You can change the timeout from it's default of 1sec"
  (binding [*eventually-timeout-ms* 2000]
    (let [a (atom -1)]
      (future
        (Thread/sleep 1500)
        (reset! a 1))
      (fact
        (deref a) =eventually=> pos?))))
```

## `contains`

The `contains` returns a predicate that matches expectations with more relaxed 
manner. For example:

```clj
(fact
  {:a 1 :b 2} => {:a 1})  ;=> FAILS
```

```clj
(fact
  {:a 1 :b 2} => (contains {:a 1}))  ;=> Ok
```

### Testing maps with `contains`

When given a map, `contains` returns a predicate that checks that all given
keys are found and they match expectations. Expected map can be deep and
can contain predicates. For example:

```clj
(fact
  {:a 1
   :b {:c 42
       :d {:e "foo"}}} 
  => (contains {:b {:c pos?
                    :d {:e string?}}}))
```

A very common use-case for this kind of testing is testing the HTTP 
responses. Here's an example.

```clj
(ns example.contains-example
  (:require [clojure.test :refer :all]
            [testit.core :refer :all]
            [clojure.string :as str]
            [clj-http.client :as http]))

(deftest test-google-response
  (fact
    (http/get "http://google.com")
    => (contains {:status 200
                  :headers {"Content-Type" #(str/starts-with? % "text/html")}
                  :body string?})))
```

### Testing sequentials with `contains`

When given a vector, `contains` returns a predicate that checks that the
expected value contains matches for each expected value, where the expected
values can be basic values, predicates, or anything `contains` accepts. For
example:

```clj
(fact
  [1 2 3] => (contains [1 pos? integer?]))
```

The matching is recursive, so this works too:

```clj
(fact
  [{:a 1, :b 1}
   {:a 2, :b 2}
   {:a 3, :b 3}]
  => (contains [{:a 1}, map?, {:b pos?}]))
```

#### ...and there can be more

If the expectation vector ends with symbol `...`, the actual vector can
contain more elements, they are just ignored. For example, these tests all
pass:

```clj
(facts
  [1 2 3] => (contains [1 2 3 ...])
  [1 2 3 4] => (contains [1 2 3 ...])
  [1 2 3 4 5] => (contains [1 2 3 ...]))
```

This does not pass:

```clj
(fact
  [1 2] => (contains [1 2 3 ...]))  ;=> FAILS
```

### When the order is not important

When `contains` is given a set, it returns a predicate that ensures that
all elements of given set are matched by _some_ value in actual set. For
example:

```clj
(facts
  [-1 0 +1] => (contains #{0 +1})
  [-1 0 +1] => (contains #{pos? neg?}))
```

## Extend via functions

The `testit` is designed to be easily extendable using plain old functions. You
can provide your own predicates, and combine your predicates with those provided
by `clojure.core` and `testit`.

Here's an example that combines `contains` and `ex-info?`: 

```clj
(fact "Match ex-info exceptions with combine"
  (throw (ex-info "oh no" {:reason "too lazy"}))
  =throws=>
  (ex-info? any (contains {:reason string?})))
```

This tests that the left side throws an `ex-info` exception, with any
message and a data that contains at least a `:reason` that must be a string.

## Extending `testit` with your own arrows

You can add your custom arrows using `clojure.test/assert-expr`. For example,
here's a simple extension that asserts that the test is completed within 1 sec.

```clj
(ns testit.your-own-arrow
  (:require [clojure.test :refer :all]
            [testit.core :refer :all]))

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
    (do (Thread/sleep 200) 42) =quickly=> 42)    ;=> PASS
  (fact
    (do (Thread/sleep 2000) 42) =quickly=> 42))  ;=> FAIL
```

In the example above, the first fact passes but the second fails after 1 sec.

## TODO

- [x] Implement `=eventually=>` for async tests
- [x] Add support to comparing seq's and lists
- [ ] Detect and complain about common mistakes like using multile `=>` forms with `fact`
- [ ] Provide better error messages when right side is a predicate
- [ ] Figure out a way to support [humane-test-output](https://github.com/pjstadig/humane-test-output) style output

## License

Copyright © 2017 [Metosin Oy](http://metosin.fi)

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
