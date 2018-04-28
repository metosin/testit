(ns testit.report
  (:require [clojure.string :as str]))

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
