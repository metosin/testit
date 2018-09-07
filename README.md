# testit [![Build Status](https://api.travis-ci.org/metosin/testit.svg?branch=master)](https://travis-ci.org/metosin/testit)

[Midje](https://github.com/marick/Midje) style assertions for `clojure.test`

Clojars dependency: `[metosin/testit "0.2.0"]`

**Note**: This library is still under heavy development!

## Goals and non-goals

*Goals:*

* Allow writing tests in Midje style with `=>` and `=not=>`
* Uses (and extends) `clojure.test`
* Allow extending functionality

*Non-goals:*

* Does not provide any compatibility with Midje
* Does not improve output

## Quick example:

```clj
(deftest test-google-response
  (fact "Google responds with 200 and some HTML"
    (http/get "http://google.com")
    =>
    {:status 200
     :headers {"Content-Type" #(str/starts-with? % "text/html")}
     :body string?}))
```

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
    {:a 1 :z 1} => {:a 1}))
```

## Extended equality

Midje has coined the term
_[extended equality](https://github.com/marick/Midje/wiki/Extended-equality)_.
The purpose of extended equality is to extend the way equality is determined
to help testing. For example, in the example above:

```clj
  {:a 1 :z 1} => {:a 1}
```

The above test passes, because the left side fulfills all key/value pairs
required by the map on the right.

The rules for extended equality in `testit` can be explained by some examples.

### Function

In extended equality functions are treated as predicates. The evaluated value
of the left side is given to the function, and if the returned value is truthy
the test passes.

```clj
  (facts "extended equality: function"
    (* 21 2) => integer?
    (* 21 2) => (partial > 1337))
```

### Map 

Every key/value pair from right side map is considered a requirement for the
evaluated value. That means that the left side may have more keys and the
test passes still.

```clj
  (fact "right side defines requirements that must be present"
    {:a 1, :b 2} => {:a 1})
```

Comparing is done recursively. 

```clj
  (fact "extended equality is tested recursively"
    {:a 1, 
     :b {:c 2}
     :d 3
     :e {:f "hello"}}
    =>
    {:b {:c 2}
     :e {:f string?}})
```

### Vector

Vectors are matched value by value.

```clj
  (fact
    [-1 0 +1] => [neg? zero? pos?])
```

If the vector ends with symbol `...`, the tested vector may contain more
elements. Note that there is nothing magical with the `...` symbol, it's
defined in the `testit.core` namespace like this:

```clj
(def ... ::and-then-some)
```

Lets see an example, the following test does **not** pass:

```clj
  (fact
    [1 2 3 4 5] => [1 2 3])
  ;=> FAIL in () (basics.clj:56)
  ;   did not expect more than 3 elements
  ;   expected: nil
  ;     actual: 4
```

but this test does pass:

```clj
  (fact
    [1 2 3 4 5] => [1 2 3 ...])
  ;=> passes
```

The `testit.core` contains some helpers, like `in-any-order` that returns
a predicate that tests that the left side contains all provided values ignoring
the order.

```clj
  (fact
    [-1 0 +1] => (in-any-order [0 1]))
```

The most simple way (but not the only way) to extend the extended equality 
testing is by creating your own custom predicate functions.

### Set

Comparing extended equality with a set is to test that the left value is
one of the values in the set on the right.

```clj
  (fact"
    :a => #{:a :b :c})
```

The above test is same as testing with `(contains? #{:a :b :c} :a)`.

### Regular expression

Testit implements extended equality for regular expressions too.

```clj
  (fact
    (str 42) => #"\d+")
```

Matching is done by `clojure.core/re-find`.

### Class

```clj
  (fact
    (/ 1 2) => clojure.lang.Ratio)
```

Matching with a class tests that the left side evaluates to an instance
of a class provided on the right side.

### Exception

```clj
  (fact
    (+ 1 "foo") =throws=> (ClassCastException. "java.lang.String cannot be cast to java.lang.Number"))
``` 

Equality with `=throws=>` arrow checks that the evaluation of the left side
throws and exception that is an instance of provided exception, and that the
messages match. Note that the messages are compared with regular equality, not
with extended equality.

The `testit.corte/exception` is an helper that could be more useful in this kind
of testing, because the the predicate returned by `exception` compares the message with
the extended equality.

```clj
  (fact
    (+ 1 "f00") =throws=> (exception ClassCastException #"String.*cast"))
```

The message is optional.

```clj
  (fact
    (+ 1 "f00") =throws=> (exception ClassCastException))
```

### ex-info

Common exception in clojure land is an `clojure.lang.ExceptionInfo` created
by `clojure.core/ex-info`. There is a built-in support for this case in
`testit`.

```clj
  (let [f (fn [] 
            (throw (ex-info "oh no" {:error 42})))]
    (fact
      (f) =throws=> (ex-info "oh no" {:error 42})))
```

Note that like with exception instances, the `ex-info` does not support
extended equality. For this purpose testit provides another helper function,
the `throws-ex-info`.

```clj
  (let [f (fn []
            (throw (ex-info "oh no" {:error 42})))]
    (fact
      (f) =throws=> (throws-ex-info #"no" {:error integer?})))
```

### nil

Testing for `nil` is supported:

```clj
  (fact
    (seq []) => nil)
```

### java.lang.Object

All other cases are tested with the `clojure.core/=`.

```clj
  (fact
    (java.net.InetAddress/getByName "localhost")
    =>
    (java.net.InetAddress/getByName "127.0.0.1"))
```

### Escape pod?

What about if you want to escape extended equality and test that the
left side matched using the normal equality? For that there is a 
`testit.core/just` that does just that (pun intended).

```clj
  (fact
    {:a 1 :b 2} => (just {:a 1 :b 2}))
``` 

The `just` is defined as follows:

```clj
; in testit.core

(defn just [expected-value]
  (partial = expected-value))
```

On the same vain, there's also helpers like:

```clj
; also in testit.core

(def any (constantly true))

(defn truthy [v]
  (if v true false))

(def falsey (complement truthy))

(defn is-not [not-expected]
  (partial not= not-expected))
```

And you could also use the existing predicates like `clojure.core/some?`
for example, or roll your own.

# Facts

The `facts` allows grouping of multiple assertions into a single form. For 
example:

```clj
(deftest group-multiple-assertions
  (facts "simple match tests"
    (+ 1 2) => 3
    (* 21 2) => 42
    (+ 623 714) => 1337))
```

# Async tests

You can use the `=eventually=>` arrow to test async code.

```clj
(let [a (atom -1)]
  (future
    (Thread/sleep 100)
    (reset! a 1))
  (fact
    @a =eventually=> pos?))
```

On the code above, evaluating the `@a` yields -1.  The test does not match 
the expected predicate `pos?`. How ever, the`=eventually=>` will keep 
repeating the test until the test matches, or a timeout occurs. Eventually 
the future resets the atom to 1 and the test passes.

By default the `=eventually=>` keeps evaluating and testing every 50 ms and 
the timeout is 1 sec. You can change these by providing your values as options
to the `fact` form:

```clj
  (let [a (atom -1)]
    (future
      (Thread/sleep 50)
      (reset! a 1))
    (fact {:timeout 200}
      @a =eventually=> pos?))
```
 
### Error messages

Testit tries to provide informative error messages. For example:

```clj
(fact
  {:a {:b {:c -1}}} => {:a {:b {:c pos?}}})
```

```
FAIL in example.basics/failing-test (basics.clj:158)
(pos? -1) => false
expected: pos?
  actual: -1
```

## TODO

- [x] Implement `=eventually=>` for async tests
- [x] Add support to comparing seq's and lists
- [ ] Detect and complain about common mistakes like using multile `=>` forms with `fact`
- [ ] Provide better error messages
- [ ] Figure out a way to support [humane-test-output](https://github.com/pjstadig/humane-test-output) style output
- [ ] Check https://github.com/jimpil/fudje
- [ ] Explain use of MultiResultResponse

## License

Copyright © 2018 [Metosin Oy](http://metosin.fi)

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
