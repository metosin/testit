(ns testit.assert)

(defmulti assert-arrow :arrow)

(defmacro format-expected [expected actual]
  `(if (fn? ~expected) '(~expected ~actual) ~expected))

