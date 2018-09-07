(ns testit.core
  (:require [clojure.test :refer [assert-expr testing do-report]]
            [testit.ext-eq :as eq])
  (:import (java.util.concurrent TimeUnit)
           (clojure.lang ExceptionInfo)))

;;
;; Arrow:
;;

(declare =>)
(declare =throws=>)
(declare =eventually=>)

;;
;; Helpers to check for passed tests:
;;

(defn pass? [response]
  (-> response :type (= :pass)))

(defn all-pass? [responses]
  (every? pass? responses))

;;
;; Common predicates and predicate factories:
;;

(def any (constantly true))

(defn truthy [v]
  (if v true false))

(def falsey (complement truthy))

(defn just [expected-value]
  (partial = expected-value))

(defn is-not [not-expected]
  (partial not= not-expected))

(defn exception
  ([ex-class] (exception ex-class nil))
  ([ex-class message]
   (fn [^Throwable e]
     (-> (concat
           (eq/accept? ex-class ex-class e [])
           (when (and e message)
             (eq/accept? message message (.getMessage e) [:message])))
         (eq/multi-result-response)))))

(defn throws-ex-info
  ([message] (throws-ex-info message nil))
  ([message data]
   (fn [^Exception e]
     (-> (concat (eq/accept? ExceptionInfo ExceptionInfo e [])
                 (when (and e message)
                   (eq/accept? message message (.getMessage e) [:message]))
                 (when (and e data)
                   (eq/accept? data data (ex-data e) [:data])))
         (eq/multi-result-response)))))

(defn in-any-order [expected-values]
  (fn [actual-values]
    (-> (reduce (fn [results [n expected-value]]
                  (concat results
                          (if-let [pass-results (some (fn [actual-value]
                                                        (let [results (eq/accept? expected-value expected-value actual-value [n])]
                                                          (when (all-pass? results)
                                                            results)))
                                                      actual-values)]
                            pass-results
                            [{:type :fail
                              :expected expected-value
                              :actual actual-values
                              :message "expected not found in actual values"
                              :path [n]}])))
                []
                (map-indexed vector expected-values))
        (eq/multi-result-response))))

;;
;; Used in sequentials with allowed extra elements:
;;

(def ... ::and-then-some)

;;
;; fact options:
;;

(def ^:dynamic default-opts
  {:polling 100
   :timeout (.toMillis TimeUnit/MINUTES 1)})

;;
;; Executing tests:
;;

(defn run-throws-test [_ expected-value expected-form actual-fn]
  (try
    (let [actual (actual-fn)]
      [{:path     []
        :type     :fail
        :message  "should throw exception"
        :expected expected-form
        :actual   actual}])
    (catch Throwable e
      (eq/accept? expected-value expected-form e []))))

(defn run-test [_ expected-value expected-form actual-fn]
  (try
    (eq/accept? expected-value expected-form (actual-fn) [])
    (catch Throwable e
      [{:path     []
        :type     :fail
        :message  "unexpected exception"
        :expected expected-form
        :actual   e}])))

(defn run-test-async [opts expected-value expected-form actual-fn]
  (let [opts (merge default-opts opts)
        now (System/currentTimeMillis)
        deadline (-> opts :timeout (+ now))]
    (loop []
      (let [actual (try
                     (deref (future-call actual-fn)
                            (- deadline (System/currentTimeMillis))
                            ::timeout)
                     (catch java.util.concurrent.ExecutionException cee
                       (.getCause cee)))]
        ; TODO: swap ::timeout and accept? tests. so that after sleep
        ; we test accept? first, timeout second
        (if (= actual ::timeout)
          [{:path []
            :type :fail
            :message "timeout"
            :expected expected-form
            :actual actual}]
          (let [result (eq/accept? expected-value expected-form actual [])]
            (if (all-pass? result)
              result
              (do (Thread/sleep (-> opts :polling))
                  (recur)))))))))

;;
;; Parse fact form into options, test name and arrow form:
;;

(defn opts-name-and-body [form]
  (let [[opts form] (if (and (-> form first map?)
                             (-> form second (not= '=>)))
                      [(first form) (rest form)]
                      [nil form])
        [name form] (if (and (-> form first string?)
                             (-> form second (not= '=>)))
                      [(first form) (rest form)]
                      [nil form])]
    [opts name form]))

;;
;; fact and facts macros:
;;


(defmacro fact [& form]
  (let [[opts test-name form] (opts-name-and-body form)
        [actual-form arrow expected-form] form
        throws? (= arrow '=throws=>)
        async? (or (= arrow '=eventually=>)
                   (:timeout opts))
        test-form `(try
                     (let [actual-fn# (fn [] ~actual-form)
                           expected-value# ~expected-form
                           run-test# ~(cond
                                        throws? `run-throws-test
                                        async? `run-test-async
                                        :else `run-test)]
                       (doseq [report# (run-test# ~opts
                                                  expected-value#
                                                  '~expected-form
                                                  actual-fn#)]
                         (do-report report#)))
                     (catch Throwable t#
                       (do-report {:type :error
                                   :message ~(str (pr-str actual-form)
                                                  " "
                                                  arrow
                                                  " "
                                                  (pr-str expected-form)
                                                  " caused an unexpected exception")
                                   :expected '~expected-form
                                   :actual t#})))]
    (if test-name
      (list 'clojure.test/testing test-name test-form)
      test-form)))

(defmacro facts [& form]
  (let [[opts test-name forms] (opts-name-and-body form)
        opts (or opts {})
        test-forms (for [fact-form (partition 3 forms)]
                     (list* 'testit.core/fact opts fact-form))]
    (if test-name
      (list* 'clojure.test/testing test-name test-forms)
      (list* 'do test-forms))))

(defmacro facts-for [& forms]
  (let [[opts test-name [actual & forms]] (opts-name-and-body forms)
        opts (or opts {})
        actual-sym (gensym)
        test-forms `(let [~actual-sym ~actual]
                      ~@(for [fact-form (partition 2 forms)]
                          (list* 'testit.core/fact opts actual-sym fact-form)))]
    (if test-name
      (list 'clojure.test/testing test-name test-forms)
      test-forms)))

