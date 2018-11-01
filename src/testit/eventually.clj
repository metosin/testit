(ns testit.eventually
  (:require [clojure.test :refer [do-report]]
            [testit.assert :refer [assert-arrow format-expected]]
            [testit.in :as in]))

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

(defmethod assert-arrow '=eventually=> [{:keys [msg expected actual]}]
  `(let [expected# (format-expected ~expected ~actual)
         result# (eventually
                   (let [e# ~expected]
                     (if (fn? e#) e# (partial = e#)))
                   (fn [] (future ~actual)))]
     (if (:success? result#)
       (do-report {:type :pass, :message ~msg, :expected expected#, :actual (:value result#)})
       (do-report {:type :fail, :message ~msg, :expected expected#, :actual (:value result#)}))))

(defn failed? [result-item]
  (-> result-item :type (not= :pass)))

(defmacro test-in-eventually [msg expected actual polling timeout]
  `(try
     (let [polling# ~polling
           deadline# (+ (System/currentTimeMillis) ~timeout)
           expected-form# (quote ~expected)
           expected-value# ~expected
           actual-fn# (fn [] ~actual)]
       (loop []
         (let [actual-value# (actual-fn#)
               result# (in/deep-compare [] expected-form# expected-value# actual-value#)
               now# (System/currentTimeMillis)]
           (if (and (< now# deadline#)
                    (some failed? result#))
             (do (Thread/sleep polling#)
                 (recur))
             (in/generate-report ~msg expected-form# actual-value# result#)))))
     (catch Throwable t#
       {:type :error
        :message ~msg
        :expected ~expected
        :actual t#})))

(defmethod assert-arrow '=eventually-in=> [{:keys [msg expected actual]}]
  `(do-report (test-in-eventually ~msg ~expected ~actual *eventually-polling-ms* *eventually-timeout-ms*)))
