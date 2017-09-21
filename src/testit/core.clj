(ns testit.core
  (:require [clojure.test :refer [assert-expr testing do-report]]
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

(defmulti assert-arrow :arrow)

(defmacro fact [& form]
  (let [[name [value arrow expected]] (name-and-body form)
        msg (or name (str (pr-str value) " " arrow " " expected))]
    `(try
       ~(assert-arrow {:arrow arrow
                       :msg msg
                       :actual value
                       :expected expected
                       :form form})
       (catch Throwable t#
         (do-report {:type :error, :message ~msg,
                     :expected '~form, :actual t#})))))

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

(defmacro format-expected [expected actual]
  `(if (fn? ~expected) '(~expected ~actual) ~expected))

(defn match-expectation? [expected actual]
  (if (fn? expected)
    (expected actual)
    (= expected actual)))

(declare =>)
(defmethod assert-arrow '=> [{:keys [expected actual msg]}]
  `(let [value# ~actual
         expected# (format-expected ~expected ~actual)]
     (if (match-expectation? ~expected value#)
       (do-report {:type :pass, :message ~msg, :expected expected#, :actual value#})
       (do-report {:type :fail, :message ~msg, :expected expected#, :actual value#}))))

(declare =not=>)
(defmethod assert-arrow '=not=> [{:keys [expected actual msg]}]
  `(let [value# ~actual
         expected# (format-expected ~expected value#)]
     (if-not (match-expectation? ~expected value#)
       (do-report {:type :pass, :message ~msg, :expected expected#, :actual value#})
       (do-report {:type :fail, :message ~msg, :expected expected#, :actual value#}))))

(declare =in=>)
(defmethod assert-arrow '=in=> [{:keys [expected actual msg]}]
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
(defmethod assert-arrow '=eventually=> [{:keys [msg expected actual]}]
  `(let [expected# (format-expected ~expected ~actual)]
     (if (eventually
          (let [e# ~expected]
            (cond
              (fn? e#) e#
              ;; TODO: add special support for futures, promises, derefs
              :else (partial = e#)))
          (fn [] ~actual))
       (do-report {:type :pass, :message ~msg, :expected expected#, :actual ~actual})
       (do-report {:type :fail, :message ~msg, :expected expected#, :actual ~actual}))))

(declare =eventually-in=>)
(defmethod assert-arrow '=eventually-in=> [{:keys [msg expected actual]}]
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
(defmethod assert-arrow '=throws=> [{:keys [expected actual msg]}]
  `(let [expected# (format-expected ~expected ~actual)]
     (try
       ~actual
       (do-report {:type :fail, :message ~msg, :expected expected#, :actual nil})
       (catch Throwable ex#
         (if (exception-match? ~expected ex#)
           (do-report {:type :pass, :message ~msg, :expected expected#, :actual ex#})
           (do-report {:type :fail, :message ~msg, :expected expected#, :actual ex#}))))))

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
