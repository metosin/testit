(ns testit.core
  (:require [clojure.test :refer [assert-expr testing do-report]]
            [clojure.spec.alpha :as s]
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

(defmulti assert-arrow :arrow)

;; Macro specs are always checked, no need to call instrument

(s/def ::fact (s/cat :name (s/? string?)
                     :value any?
                     :arrow symbol?
                     :expected any?))

(s/fdef fact
  :args ::fact)

(defmacro fact [& form]
  (let [{:keys [name value arrow expected]} (s/conform ::fact form)
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

(s/def ::facts (s/cat :name (s/? string?)
                      :body (s/* (s/cat :value any?
                                        :arrow symbol?
                                        :expected any?))))

(s/fdef facts
  :args ::facts)

(defmacro facts [& form]
  (let [{:keys [name body]} (s/conform ::facts form)]
    `(testing ~name
       ~@(for [{:keys [value arrow expected]} body]
           `(fact ~value ~arrow ~expected)))))

(s/def ::facts-for (s/cat :name (s/? string?)
                          :form-to-test any?
                          :fact-forms (s/* (s/cat :arrow symbol?
                                                  :expected any?))))

(s/fdef facts-for
  :args ::facts-for)

(defmacro facts-for [& forms]
  (let [{:keys [name form-to-test fact-forms]} (s/conform ::facts-for forms)
        result (gensym)]
    `(testing ~name
       (let [~result ~form-to-test]
         ~@(for [{:keys [arrow expected]} fact-forms]
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
        deadline (+ (System/currentTimeMillis) *eventually-timeout-ms*)
        time-until-deadline (fn [] (- deadline (System/currentTimeMillis)))]
    (loop [time-left (time-until-deadline)]
      (let [v (try
                (deref (actual)
                       time-left
                       ::timeout)
                (catch java.util.concurrent.ExecutionException e
                  (.getCause e)))
            r (try
                (expected v)
                (catch Exception _
                  nil))]
        (if r
          {:success? true, :value v}
          (do (Thread/sleep polling)
              (let [time-left (time-until-deadline)]
                (if (pos? time-left)
                  (recur time-left)
                  {:success? false, :value v}))))))))

(declare =eventually=>)
(defmethod assert-arrow '=eventually=> [{:keys [msg expected actual]}]
  `(let [expected# (format-expected ~expected ~actual)
         result# (eventually
                   (let [e# ~expected]
                     (if (fn? e#) e# (partial = e#)))
                   (fn [] (future ~actual)))]
     (if (:success? result#)
       (do-report {:type :pass, :message ~msg, :expected expected#, :actual (:value result#)})
       (do-report {:type :fail, :message ~msg, :expected expected#, :actual (:value result#)}))))

(declare =eventually-in=>)
(defmethod assert-arrow '=eventually-in=> [{:keys [msg expected actual]}]
  `(do-report (in/test-in-eventually ~msg ~expected ~actual *eventually-polling-ms* *eventually-timeout-ms*)))

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
                                        (= (.getMessage ^Throwable expected)
                                           (.getMessage ^Throwable exception)))
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

(defn ex-message?
  "Checks that a Throwable is thrown and the message matches given string, regex or predicate."
  [message]
  (let [message-check (cond
                        (instance? java.util.regex.Pattern message) #(re-find message %)
                        (fn? message) message
                        :else (partial = message))]
    (fn [e]
      (and (instance? Throwable e)
           (message-check (.getMessage ^Throwable e))))))

(defn ex-info?
  "Checks that a ExceptionInfo is thrown and the message and data match.
  Data is checked using deep compare, similar to => arrow.
  Use `any` to ignore message."
  [message data]
  {:pre [(or (nil? message)
             (string? message)
             (fn? message))
         (or (nil? data)
             (map? data)
             (fn? data))]}
  (let [message-check (ex-message? message)
        data-check (fn [actual]
                     (every? (comp (partial = :pass) :type)
                             (in/deep-compare [] data data actual)))]
    (fn [e]
      (and (instance? IExceptionInfo e)
           (message-check e)
           (data-check (.getData ^IExceptionInfo e))))))

(defn cause-ex-info?
  "Checks that a Throwable is thown and that cause stack contains IExceptionInfo matching
  ex-info? check."
  [message data]
  (let [ex-info-check (ex-info? message data)]
    (fn [e]
      (and (instance? Throwable e)
           (let [t (loop [t e]
                     (if (instance? IExceptionInfo t)
                       t
                       (if-let [c (.getCause t)]
                         (recur c))))]
             (ex-info-check t))))))
