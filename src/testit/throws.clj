(ns testit.throws
  (:require [clojure.test :refer [do-report]]
            [testit.assert :refer [assert-arrow format-expected]]
            [testit.in :as in])
  (:import (clojure.lang IExceptionInfo)))

;;
;; =throws=>
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
                        (every? boolean))))

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
;; ex-info helper:
;;

(defn ex-info? [message data]
  {:pre [(or (nil? message)
             (string? message)
             (fn? message)
             (instance? java.util.regex.Pattern message))
         (or (nil? data)
             (map? data)
             (fn? data))]}
  (let [message-check (cond
                        (instance? java.util.regex.Pattern message) (partial re-find message)
                        (fn? message) message
                        :else (partial = message))
        data-check (fn [actual]
                     (every? (comp (partial = :pass) :type)
                             (in/deep-compare [] data data actual)))]
    (fn [e]
      (and (instance? IExceptionInfo e)
           (message-check (.getMessage ^Throwable e))
           (data-check (.getData ^IExceptionInfo e))))))
