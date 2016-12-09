(ns testit.facts
  (:require [clojure.test :refer :all]
            [testit.core]))

(defn- name-and-body [form]
  (if (string? (first form)) ((juxt first rest) form) [nil form]))

(defmacro fact [& form]
  (let [[name [value arrow expected]] (name-and-body form)]
    `(is (~arrow ~expected ~value) ~(or name (str value " " arrow " " expected)))))

(defmacro facts [& form]
  (let [[name body] (name-and-body form)]
    `(testing ~name
       ~@(for [[value arrow expected] (partition 3 body)]
           `(fact ~value ~arrow ~expected)))))

(declare =>)
(defmethod assert-expr '=> [msg [_ & body]]
  (assert-expr msg (concat (if-not (function? (first body))
                             (list 'same))
                           body)))

(declare =not=>)
(defmethod assert-expr '=not=> [msg [_ & body]]
  (assert-expr msg (if (function? (first body))
                     (conj (rest body)
                           `(~'complement ~(first body)))
                     (conj body 'not=))))
