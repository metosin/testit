(ns testit.core
  (:require [clojure.test :refer [assert-expr testing do-report]]
            [clojure.spec.alpha :as s]
            [testit.in :as in]
            [testit.assert :refer [assert-arrow format-expected]]
            [net.cgrand.macrovich :as macros]
            #?(:clj [testit.eventually])
            #?(:clj [testit.throws])))

;;
;; Common predicates:
;;

(def any (constantly true))
(defn truthy [v] (if v true false))
(defn falsey [v] (if-not v true false))

;;
;; fact and facts macros:
;;

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
       (catch ~(macros/case :clj Throwable :cljs :default) t#
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
;; Used in =in=> with sequentials:
;;

#?(:clj (def ... ::and-then-some))

;;
;; Re-exports and declarations for Clojure
;;

#?(:clj (def ex-info? testit.throws/ex-info?))
#?(:clj (declare =eventually=> =eventually-in=>))
#?(:clj (declare =throws=>))
