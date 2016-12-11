(ns testit.facts
  (:require [clojure.test :refer :all]))

(defn- name-and-body [form]
  (if (and (-> form first string?)
           (-> form count (mod 3) (= 1)))
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
           (or (symbol? (first body))
               (fn? (first body))))
      (and (seq? body)
           (seq? (first body))
           (or (symbol? (ffirst body))
               (fn? (ffirst body))))))

(declare =>)
(defmethod assert-expr '=> [msg [_ & body]]
  (assert-expr msg (if (expected-fn? body)
                     body
                     (cons `= body))))

(declare =not=>)
(defmethod assert-expr '=not=> [msg [_ & body]]
  (assert-expr msg (cons `not (list (if (expected-fn? body)
                                      body
                                      (cons `= body))))))

(defn exception-match? [expected exception]
  (cond
    (class? expected) (instance? expected exception)
    (instance? Throwable expected) (and (instance? (class expected) exception)
                                        (= (.getMessage expected)
                                           (.getMessage exception)))
    (fn? expected) (expected exception)))

(declare =throws=>)
(defmethod assert-expr '=throws=> [msg [_ e & body]]
  (assert-expr msg `(try
                      ~@body
                      (catch Throwable ex#
                        (exception-match? ~e ex#)))))
