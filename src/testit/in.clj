(ns testit.in
  (:require [clojure.test :refer :all]
            [clojure.string :as str])
  (:import (clojure.lang IExceptionInfo ILookup Associative Seqable)))

;;
;; Utils:
;;

(defn- seqable? [v]
  (instance? Seqable v))

(defn map-like? [v]
  false)

(defn deep-compare [path e a]
  (let [e' (eval e)]
    (cond
      ; expected is fn:
      (fn? e')
      (let [r (e' a)]
        [{:path path
          :type (if r :pass :fail)
          :message (format "(%s %s) => %s" (name e) (pr-str a) (pr-str r))
          :expected e
          :actual a}])

      ; both are sequential:
      (and (sequential? e') (sequential? a))
      (mapcat deep-compare
              (map (partial conj path) (range))
              e
              a)

      ; both are map(like):
      (and (associative? a) (associative? e'))
      (reduce-kv (fn [acc k v]
                   (let [path' (conj path k)]
                     (concat acc (if (contains? a k)
                                   (deep-compare path' v (get a k))
                                   [{:path path'
                                     :type :fail
                                     :message (format "actual is missing key %s" k)
                                     :expected v
                                     :actual nil}]))))
                 []
                 e)

      ; none of the above, use =:
      :else
      (let [r (= e' a)]
        [{:path path
          :type (if r :pass :fail)
          :message (format "(= %s %s) => %s" (pr-str e') (pr-str a) (pr-str r))
          :expected e
          :actual a}]))))


(defn explain-errors [result]
  (->> result
       (remove (comp (partial = :pass) :type))
       (map (fn [{:keys [path message expected actual]}]
              (format "at %s \"%s\"\n  expected: %s\n  actual: %s"
                      (pr-str path)
                      message
                      (pr-str expected)
                      (pr-str actual))))
       (str/join "\n")))

(defn generate-report [msg expected actual result]
  (let [total-type (if (->> result (map :type) (every? (partial = :pass)))
                     :pass
                     :fail)
        message (if (= total-type :pass)
                  msg
                  (str msg ":\n" (explain-errors result)))]
    {:type total-type
     :message message
     :expected expected
     :actual actual}))

(defmacro test-in [msg expected actual]
  `(try
     (let [e# (quote ~expected)
           a# ~actual]
       (->> (deep-compare [] e# a#)
            (generate-report ~msg e# a#)
            (do-report)))
     (catch Throwable t#
       (do-report {:type :error
                   :message ~msg
                   :expected ~expected
                   :actual t#}))))

(-> (test-in "some fancy test"
             [1 [pos? neg?] 3 {:a 1 :b pos? :c 3}]
             [1 [2 -2] 13 {:a 1 :b -1}])
    :message
    println)

(test-in "some message"
         1 2)

(test-in "some message"
         {:a pos?
          :b [string? "bozz" zero?]
          :c 42}
         {:a 42
          :b ["foo" "boz" (- 2 2 1)]
          :c 41})
