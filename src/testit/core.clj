(ns testit.core
  (:require [clojure.test :refer :all])
  (:import (clojure.lang IExceptionInfo ILookup Associative Seqable)))

;;
;; Common predicates:
;;

(def any (constantly true))
(defn truthy [v] (if v true false))
(defn falsey [v] (if-not v true false))

;;
;; Utils:
;;

(defn- map-like? [v]
  (and (instance? ILookup v)
       (instance? Associative v)))

(defn- seqable? [v]
  (instance? Seqable v))

(defn- deep-compare [actual expected]
  (cond
    ; they are equal?
    (= actual expected)
    true
    ; expected is fn?
    (or (symbol? expected)
        (fn? expected))
    (expected actual)
    ; both are map(like):
    (and (map-like? actual)
         (map-like? expected))
    (and (= (keys actual) (keys expected))
         (reduce-kv (fn [match? k expected-v]
                      (and match?
                           (contains? actual k)
                           (deep-compare (get actual k) expected-v)))
                    true
                    expected))
    ; both are sequable:
    (and (seqable? actual)
         (seqable? expected))
    (->> (map deep-compare actual expected)
         (every? truthy))
    ; none of the above, fail:
    :else false))

;;
;; fact and facts macros:
;;

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

(defmacro facts-for [& forms]
  (let [[name [form-to-test & fact-forms]] (if (and (-> forms first string?)
                                                    (-> forms count dec (mod 2) (= 1)))
                                             ((juxt first rest) forms)
                                             [nil forms])
        result (gensym)]
    `(testing ~name
       (let [~result ~form-to-test]
         ~@(for [[arrow expected] (partition 2 fact-forms)]
             `(fact ~result ~arrow ~expected))))))

;;
;; Extending clojure.test for =>, =not=> and =throw=>
;;

(defn match-expectation? [expected actual]
  (if (fn? expected)
    (expected actual)
    (= expected actual)))

(declare =>)
(defmethod assert-expr '=> [msg [_ expected actual]]
  (assert-expr msg (list `match-expectation? expected actual)))

(declare =not=>)
(defmethod assert-expr '=not=> [msg [_ expected actual]]
  (assert-expr msg (list (list `complement `match-expectation?) expected actual)))

(def ^:dynamic *eventually-polling-ms* 50)
(def ^:dynamic *eventually-timeout-ms* 1000)

(defn eventually [expected actual]
  (let [polling *eventually-polling-ms*
        deadline (+ (System/currentTimeMillis) *eventually-timeout-ms*)]
    (loop []
      (let [v (actual)
            r (expected v)]
        (cond
          r r
          (< (System/currentTimeMillis) deadline) (do (Thread/sleep polling)
                                                      (recur))
          :else false)))))

(declare =eventually=>)
(defmethod assert-expr '=eventually=> [msg [_ expected actually]]
  (assert-expr msg `(eventually
                      (let [e# ~expected]
                        (cond
                          (fn? e#) e#
                          ; TODO: add special support for futures, promises, derefs
                          :else (partial = e#)))
                      (fn [] ~actually))))


(defn cause-seq [^Throwable exception]
  (if exception
    (lazy-seq
      (cons exception
            (cause-seq (.getCause exception))))))

(defn exception-match? [expected exception]
  (cond
    (class? expected) (instance? expected exception)
    (instance? Throwable expected) (and (instance? (class expected) exception)
                                        (= (.getMessage expected)
                                           (.getMessage exception)))
    (fn? expected) (expected exception)
    (map? expected) (and (instance? IExceptionInfo exception)
                         (deep-compare (.getData ^IExceptionInfo exception) expected))
    (seq expected) (->> (map exception-match? expected (cause-seq exception))
                        (every? truthy))))

(declare =throws=>)
(defmethod assert-expr '=throws=> [msg [_ e & body]]
  (assert-expr msg `(try
                      ~@body
                      (is false "Should throw")
                      (catch Throwable ex#
                        (exception-match? ~e ex#)))))

;;
;; contains helper:
;;

(declare contains)

(defmulti vector-checker (fn [key _ _] key) :default ::default)

(defmethod vector-checker ::default [_ _ _] nil)
(defmethod vector-checker ::and-then-some [_ actual expected]
  (reduce (fn [match? [actual-v expected-v]]
            (and match? (deep-compare actual-v (contains expected-v))))
          true
          (map vector actual expected)))

(def ... ::and-then-some)

(defn contains [expected]
  (cond
    (map? expected)
    (fn [actual]
      (and (map-like? actual)
           (reduce-kv (fn [match? k expected-v]
                        (and match?
                             (contains? actual k)
                             (deep-compare (get actual k) (contains expected-v))))
                      true
                      expected)))

    (set? expected)
    (fn [actual]
      (and (<= (count expected) (count actual))
           (reduce (fn [match? expected-v]
                     (and match? (true? (some (contains expected-v) actual))))
                   true
                   expected)))

    (vector? expected)
    (fn [actual]
      (let [maybe-checked (vector-checker (last expected) actual (butlast expected))]
        (if maybe-checked
          (or (<= (dec (count expected)) (count actual))
              maybe-checked)
          (and (= (count actual) (count expected))
               (reduce (fn [match? [actual-v expected-v]]
                         (and match? (deep-compare actual-v (contains expected-v))))
                       true
                       (map vector actual expected))))))

    (fn? expected)
    (fn [actual]
      (expected actual))

    :else
    (fn [actual]
      (= actual expected))))

;;
;; ex-info helper:
;;

(defn ex-info? [message data]
  {:pre [(or (nil? message)
             (string? message)
             (fn? message))
         (or (nil? data)
             (map? data)
             (fn? data))]}
  (let [message-check (if (fn? message)
                        message
                        (partial = message))
        data-check (if (fn? data)
                     data
                     (partial = data))]
    (fn [e]
      (and (instance? IExceptionInfo e)
           (message-check (.getMessage ^Throwable e))
           (data-check (.getData ^IExceptionInfo e))))))

