# testit

**WIP** Midje style assertions for Clojure.test

## Usage

```clj
(facts
  {:a 1 :z 1} => (contains {:a 1})
  {:a 1 :sub {:b 1 :foo :bar}} => (contains {:a 1 :sub (contains {:b 1})}))

; ==>

(deftest same-test
  (is (same (contains {:a 1})
            {:a 1 :z 1})
  (is (same (contains {:a 1 :sub (contains {:b 1})})
            {:a 1 :sub {:b 1 :foo :bar}})))
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
