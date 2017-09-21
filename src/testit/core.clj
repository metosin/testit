(ns testit.core
  (:require [clojure.test :refer :all]
            [testit.in :as in])
  (:import (clojure.lang IExceptionInfo)))

;;
;; Common predicates:
;;

(def any (constantly true))
(defn truthy [v] (if v true false))
(defn falsey [v] (if-not v true false))

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
;; Extending clojure.test for => and =not=>
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

(declare =in=>)
(defmethod assert-expr '=in=> [msg [_ expected actual]]
  `(do-report (in/test-in ~msg ~expected ~actual)))

;;
;; =eventually=>
;;

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


(declare =eventually-in=>)
(defmethod assert-expr '=eventually-in=> [msg [_ expected actual]]
  `(do-report (in/test-in-eventually ~msg ~expected ~actual ~*eventually-polling-ms* ~*eventually-timeout-ms*)))

;;
;; =throes=>
;;

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
                         (in/deep-compare
                           nil
                           expected
                           expected
                           (.getData ^IExceptionInfo exception)))
    (seq expected) (->> (map exception-match? expected (cause-seq exception))
                        (every? truthy))))

(declare =throws=>)
(defmethod assert-expr '=throws=> [msg [_ e & body]]
  (assert-expr msg `(try
                      ~@body
                      (is false "Expected an exception")
                      (catch Throwable ex#
                        (exception-match? ~e ex#)))))

;;
;; Used in =in=> with sequentials:
;;

(def ... ::and-then-some)

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
        data-check (fn [actual]
                     (every? (comp (partial = :pass) :type)
                             (in/deep-compare [] data data actual)))]
    (fn [e]
      (and (instance? IExceptionInfo e)
           (message-check (.getMessage ^Throwable e))
           (data-check (.getData ^IExceptionInfo e))))))
