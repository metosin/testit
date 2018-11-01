(ns testit.macros
  (:require [testit.in :as in]))

(defmacro deep [expected actual]
  `(in/deep-compare nil (quote ~expected) ~expected ~actual))
