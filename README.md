# testit

(Midje)[https://github.com/marick/Midje] style assertions for `clojure.test`

# Goals and non-goals

*Goals:*

* Allow writing tests in Midje style with `=>` and `=not=>`
* Use (and extends) `clojure.test`

*Non-goals:*

* Does not provide 100% compatibility with Midje
* Does not improve output

## Quick intro for Midje users

with Midje:

```clj
(require '[midje.sweer :refer :all])

(facts
  (+ 1 2) => 3
  {:a 1 :z 1} => (contains {:a 1}))
```

with `testit.facts`:

Add dependency to `[metosin/testit "0.1.0-SNAPSHOT"]`

```clj
(require '[clojure.test :refer [deftest])
(require '[testit.facts :refer [facts])
(require '[testit.core :refer [contains])

(deftest midje-impersonation
  (facts
    (+ 1 2) => 3
    {:a 1 :z 1} => (contains {:a 1})))
```

## More realistic example:

Assuming you have function `GET` that calls your REST service, you
could write a test like this:

```clj
(ns app.rest-test
  (:require [clojure.test :refer :all]
            [testit.facts :refer [facts]]
            [testit.response :as r]
            [app.rest :refer [GET]]))

(deftest hello-returns-with-200
  (let [response (GET "/hello")]
    (facts
      response => (r/status 200)
      response => (r/content-type "text/plain")
      response => (r/body string?))))
```


## How does it work?

`testit` uses `clojure.test/assert-expr` to add support for `=>` and `=not=>`
symbols and the `testit.facts/fact` and `testit.facts/facts` macros generate 
`clojure.test/is` forms.

```clj
(macroexpand-1 '(fact (+ 1 2) => 3))
=> (clojure.test/is (=> 3 (+ 1 2)) "(+ 1 2) => 3")
```

## TODO

- [ ] Instead of extending clojure.data protocols, should reimplement diff and the protocols
  - Extending protocols is global side-effect and could case problems
  - `[only-a only-b both]` is maybe not the best format for this case
- [ ] Devise way to support [humane-test-output](https://github.com/pjstadig/humane-test-output)
  style output
- [ ] `(same (contains [1 2]) [1 2 3])`
- [ ] `(same (contains [{:a 1}]) [{:a 1 :z 1}])`
- [ ] `(contains ... :in-any-order)`
- [ ] `(contains ... :gapps-ok)`
- [ ] Check that useful errors are given if `same` assertion is not used correctly (wrong number of args etc.)
- [ ] Other assertions?
  - Check that thrown Exception has specific `ex-data`
  - Check that thrown Exception has specific message

## License

Copyright © 2015 [Metosin Oy](http://metosin.fi)

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
