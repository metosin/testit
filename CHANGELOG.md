# Changelog

## 0.4.1 (2021-09-03)

**[compare](https://github.com/metosin/testit/compare/0.4.0...0.4.1)**

- Add style/indent metadata to the three fact* macros [#19](https://github.com/metosin/testit/pull/19)
- Fix the ClojureScript support [commit](https://github.com/metosin/testit/commit/2938e75c9716af6cc731b2bac618a445a47f3277)

## 0.4.0 (2019-06-08)

**[compare](https://github.com/metosin/testit/compare/0.3.0...0.4.0)**

- ClojureScript support for `=>` and `=in=>`.

## 0.3.0 (2018-09-21)

**[compare](https://github.com/metosin/testit/compare/0.2.2...0.3.0)**

- Validate `fact`, `fact` and `facts-for` parameters using Spec
- Support regex as exception message check

## 0.2.2 (2018-04-26)

**[compare](https://github.com/metosin/testit/compare/0.2.1...0.2.2)**

- `=eventually-in=>` arrow now works with `testit.core/*eventually-timeout-ms*` / `testit.core/*eventually-polling-ms*` bindings ([#10](https://github.com/metosin/testit/pull/10))
- Improved `=eventually=>`
