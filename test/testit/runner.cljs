(ns testit.runner
  (:require
    [doo.runner :refer-macros [doo-tests]]
    [testit.facts-test]
    [testit.in-test]))

(doo-tests 'testit.facts-test
           'testit.in-test)
