(ns testit.in
  (:require [clojure.test :refer :all]
            [clojure.string :as str]))

(defn deep-compare [path expected-form expected-value actual]
  (cond
    ; expected is fn:
    (fn? expected-value)
    (let [r (expected-value actual)]
      [{:path path
        :type (if r :pass :fail)
        :message (format "(%s %s) => %s" expected-form (pr-str actual) (pr-str r))
        :expected expected-form
        :actual actual}])

    ; expected is vector:
    (vector? expected-value)
    (cond
      (not (sequential? actual))
      [{:path path
        :type :fail
        :message "expected sequential"
        :expected expected-form
        :actual actual}]

      (-> expected-value meta :in-any-order)
      (reduce (fn [acc [path expected-form expected-value]]
                (conj acc (if-let [matched-value (some (fn [actual-value]
                                                         (if (->> (deep-compare nil expected-form expected-value actual-value)
                                                                  (every? (comp (partial = :pass) :type)))
                                                           actual-value))
                                                       actual)]
                            {:path path
                             :type :pass
                             :message (str (pr-str matched-value) " matches " expected-form)
                             :expected expected-form
                             :actual matched-value}
                            {:path path
                             :type :fail
                             :message (str "nothing matches " expected-form)
                             :expected expected-form
                             :actual nil})))
              []
              (map vector
                   (map (partial conj path) (range))
                   expected-form
                   expected-value))

      :else
      (reduce (fn [acc [path expected-form expected-value actual]]
                (cond
                  ; expected is ..., that means we're done:
                  (= expected-value :testit.core/and-then-some)
                  (reduced acc)

                  ; both ended at the same time:
                  (= expected-value actual ::eof)
                  (reduced acc)

                  ; expected shorter than actual:
                  (and (= expected-value ::eof)
                       (not= actual ::eof))
                  (reduced (conj acc {:path path
                                      :type :fail
                                      :message (format "did not expect more than %d elements" (last path))
                                      :expected nil
                                      :actual actual}))

                  ; expected longer than actual:
                  (and (not= expected-value ::eof)
                       (= actual ::eof))
                  (reduced (conj acc {:path path
                                      :type :fail
                                      :message (format "expected more than %d elements" (last path))
                                      :expected expected-form
                                      :actual nil}))

                  ; regular deep compare:
                  :else
                  (into acc (deep-compare path expected-form expected-value actual))))
              []
              (map vector
                   (map (partial conj path) (range))
                   (concat expected-form [::eof])
                   (concat expected-value [::eof])
                   (concat actual [::eof]))))

    ; expected is map(like):
    (associative? expected-value)
    (if (not (associative? actual))
      [{:path path
        :type :fail
        :message "expected associative"
        :expected expected-form
        :actual actual}]
      (reduce (fn [acc k]
                (let [path' (conj path k)]
                  (into acc (if (contains? actual k)
                              (deep-compare path'
                                            (get expected-form k)
                                            (get expected-value k)
                                            (get actual k))
                              [{:path path'
                                :type :fail
                                :message (format "actual is missing key %s" k)
                                :expected (get expected-form k)
                                :actual nil}]))))
              []
              (keys expected-value)))

    ; none of the above, use plain =
    :else
    (let [r (= expected-value actual)]
      [{:path path
        :type (if r :pass :fail)
        :message (format "(= %s %s) => %s"
                         (pr-str expected-form)
                         (pr-str actual)
                         (pr-str r))
        :expected expected-form
        :actual actual}])))

(defn- make-str [n c]
  (apply str (repeat n c)))

(defn equal-prefix-len [expected-value actual]
  (loop [[e & more-e] expected-value
         [a & more-a] actual
         i 0]
    (if (and e a (= e a))
      (recur more-e more-a (inc i))
      i)))

(defn- diff-str [expected-value actual]
  (let [equal-len (equal-prefix-len expected-value actual)
        diff-len (- (count actual) equal-len)]
    (str \space
         (make-str equal-len \space)
         (make-str diff-len \^))))

(defn explain-errors [result]
  (->> result
       (remove (comp (partial = :pass) :type))
       (map (fn [{:keys [path message expected actual]}]
              (format (str "  %s  %s\n"
                           "    expected: %s\n"
                           "      actual: %s"
                           (if (and (string? expected) (string? actual))
                             "\n        diff: %s"))
                      (if (seq path)
                        (str "in " (pr-str path) ":\n")
                        "")
                      message
                      (pr-str expected)
                      (pr-str actual)
                      (if (and (string? expected) (string? actual))
                        (diff-str expected actual)))))
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
     (let [expected-form# (quote ~expected)
           expected-value# ~expected
           actual-value# ~actual]
       (->> (deep-compare [] expected-form# expected-value# actual-value#)
            (generate-report ~msg expected-form# actual-value#)))
     (catch Throwable t#
       {:type :error
        :message ~msg
        :expected ~expected
        :actual t#})))
