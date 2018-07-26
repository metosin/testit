(ns testit.ext-eq
  "Extended equality")

;;
;; MultiResultResponse
;;

(defrecord MultiResultResponse [results])

(defn multi-result-response? [response]
  (instance? MultiResultResponse response))

(defn multi-result-response [results]
  (->MultiResultResponse results))

;;
;; ExtendedEquality
;;

(defprotocol ExtendedEquality
  "Protocol for extended equality"
  (accept? [expected-value expected-form actual path]
    "compare the `expected-value` to `actual` using the extended equality."))

(extend-protocol ExtendedEquality
  clojure.lang.APersistentMap
  (accept? [expected-value expected-form actual path]
    (if (not (associative? actual))
      [{:path path
        :type :fail
        :message (format "(associative? %s) => false" (pr-str actual))
        :expected expected-form
        :actual actual}]
      (->> (keys expected-value)
           (reduce (fn [acc k]
                     (let [path' (conj path k)]
                       (into acc (if (contains? actual k)
                                   (accept? (get expected-value k)
                                            (get expected-form k)
                                            (get actual k)
                                            path')
                                   [{:path path'
                                     :type :fail
                                     :message (format "actual is missing key %s" (pr-str k))
                                     :expected (get expected-form k)
                                     :actual nil}]))))
                   []))))

  clojure.lang.APersistentVector
  (accept? [expected-value expected-form actual path]
    (if-not (sequential? actual)
      [{:path path
        :type :fail
        :message (format "(sequential? %s) => false" (pr-str actual))
        :expected expected-form
        :actual actual}]
      (->> (map vector
                (concat expected-value [::eof])
                (concat expected-form [::eof])
                (concat actual [::eof])
                (map (partial conj path) (range)))
           (reduce (fn [acc [expected-value expected-form actual path]]
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
                       (into acc (accept? expected-value expected-form actual path))))
                   []))))

  clojure.lang.APersistentSet
  (accept? [expected-value expected-form actual path]
    (let [result (contains? expected-value actual)]
      [{:path path
        :type (if result :pass :fail)
        :message (format "(contains? %s %s) => %s"
                         (pr-str expected-form)
                         (pr-str actual)
                         (pr-str result))
        :expected expected-form
        :actual actual}]))

  clojure.lang.IFn
  (accept? [expected-value expected-form actual path]
    (let [[status response] (try
                              [:response (expected-value actual)]
                              (catch Throwable e
                                [:ex e]))
          result (and (= status :response) response)]
      (if (multi-result-response? response)
        (->> response
             :results
             (mapv #(update % :path (partial into path))))
        [{:path path
          :type (if result :pass :fail)
          :message (format "(%s %s) => %s"
                           (pr-str expected-form)
                           (pr-str actual)
                           (if (= status :response)
                             (pr-str response)
                             (format "exception: %s (message=%s, data=%s)"
                                     (-> response class .getName)
                                     (-> response .getMessage pr-str)
                                     (if (instance? clojure.lang.ExceptionInfo response)
                                       (-> ^clojure.lang.ExceptionInfo response
                                           .getData
                                           pr-str)
                                       "nil"))))
          :expected expected-form
          :actual actual}])))

  java.util.regex.Pattern
  (accept? [expected-value expected-form actual path]
    (let [result (and (string? actual)
                      (re-find expected-value actual))]
      [{:path path
        :type (if result :pass :fail)
        :message (format "(re-find %s %s) => %s"
                         (pr-str expected-form)
                         (pr-str actual)
                         (pr-str result))
        :expected expected-form
        :actual actual}]))

  java.lang.Class
  (accept? [expected-value expected-form actual path]
    (let [result (instance? expected-value actual)]
      [{:path path
        :type (if result :pass :fail)
        :message (if-not result
                   (format "expected instance of %s, but got %s"
                           (some-> expected-value .getName)
                           (some-> actual class .getName)))
        :expected expected-form
        :actual actual}]))

  clojure.lang.ExceptionInfo
  (accept? [expected-value expected-form actual path]
    (if-not (instance? clojure.lang.ExceptionInfo actual)
      [{:path path
        :type :fail
        :message (format "expected instance of clojure.lang.ExceptionInfo, but got %s"
                         (some-> actual class .getName))
        :expected expected-form
        :actual actual}]
      (concat
        (accept? (.getMessage expected-value)
                 (.getMessage expected-value)
                 (.getMessage ^clojure.lang.ExceptionInfo actual)
                 (conj path :message))
        (accept? (.getData expected-value)
                 (.getData expected-value)
                 (.getData ^clojure.lang.ExceptionInfo actual)
                 (conj path :data)))))

  java.lang.Throwable
  (accept? [expected-value expected-form actual path]
    (if (not (instance? java.lang.Throwable actual))
      [{:path path
        :type :fail
        :message (format "expected instance of %s, but got %s"
                         (some-> expected-value class .getName)
                         (some-> actual class .getName))
        :expected expected-form
        :actual actual}]
      (accept? (.getMessage expected-value)
               (.getMessage expected-value)
               (.getMessage ^Throwable actual)
               (conj path :message))))

  nil
  (accept? [expected-value expected-form actual path]
    (let [result (nil? actual)]
      [{:path path
        :type (if result :pass :fail)
        :message (format "(nil? %s) => %s"
                         (pr-str actual)
                         (pr-str result))
        :expected expected-form
        :actual actual}]))

  Object
  (accept? [expected-value expected-form actual path]
    (let [r (= expected-value actual)]
      [{:path path
        :type (if r :pass :fail)
        :message (format "(= %s %s) => %s"
                         (pr-str expected-form)
                         (pr-str actual)
                         (pr-str r))
        :expected expected-form
        :actual actual}])))
