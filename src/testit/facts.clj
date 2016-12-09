(ns testit.facts
  (:require [clojure.test :refer :all]
            [testit.core]))

(defn- name-and-body [form]
  (if (and (-> form first string?)
           (-> form second #{'=> '=not=>} not))
    ((juxt first rest) form)
    [nil form]))

(defmacro fact [& form]
  (let [[name [value arrow expected]] (name-and-body form)]
    `(is (~arrow ~expected ~value) ~(or name (str value " " arrow " " expected)))))

(defmacro facts [& form]
  (let [[name body] (name-and-body form)]
    `(testing ~name
       ~@(for [[value arrow expected] (partition 3 body)]
           `(fact ~value ~arrow ~expected)))))

(defn- expected-fn? [body]
  (or (and (seq? body)
           (symbol? (first body)))
      (and (seq? body)
           (seq? (first body))
           (symbol? (ffirst body)))))

(declare =>)
(defmethod assert-expr '=> [msg [_ & body]]
  (assert-expr msg (if (expected-fn? body)
                     body
                     (cons 'same body))))

(declare =not=>)
(defmethod assert-expr '=not=> [msg [_ & body]]
  (assert-expr msg (if (expected-fn? body)
                     (cons `not (list body))
                     ; TODO: can't use 'same here
                     (cons `not (list (cons `= body))))))
