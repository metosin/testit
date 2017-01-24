(ns example.your-own-arrow
  (:require [clojure.test :refer :all]
            [testit.core :refer :all]))

(declare =quickly=>)
(defmethod assert-expr '=quickly=> [msg [_ & body]]
  (assert-expr msg `(let [d# (future ~@body)
                          r# (deref d# 1000 ::timeout)]
                      (if (= r# ::timeout)
                        false
                        r#))))

(deftest things-must-be-fast-tests
  (fact
    (do (Thread/sleep 200) 42) =quickly=> 42)
  ; Will fail
  ; (fact
  ;   (do (Thread/sleep 2000) 42) =quickly=> 42)
  )
