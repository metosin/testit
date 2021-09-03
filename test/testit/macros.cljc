(ns testit.macros
  (:require [testit.in :as in])
  #?(:cljs (:require-macros testit.macros)))

#?(:clj (defmacro deep [expected actual]
          `(in/deep-compare nil (quote ~expected) ~expected ~actual)))
