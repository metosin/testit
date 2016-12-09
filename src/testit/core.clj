(ns testit.core
  (:require [clojure.data :as data]
            [clojure.set :as set]
            [clojure.test :refer :all]))

; TODO: Extending clojure.data will cause side-effects. We should probably
; make copy of everything and extend that.
(defrecord Contains [content]
  data/Diff
  (diff-similar [this b]
    (case (data/equality-partition this)
      ; If "expected" is marked contains, the other value only needs to contain keys also in expected
      :map (data/diff-similar content (select-keys b (keys content)))
      ; Select items in value which are also in expected
      ; FIXME: Doesn't work with recursive contains
      :set (data/diff-similar content (set/select content b))))
  data/EqualityPartition
  (equality-partition [this]
    (data/equality-partition content)))

(defmethod print-method Contains [v ^java.io.Writer w]
  (.write w (str "(contains " (pr-str (:content v)) ")")))

(defn contains [x]
  {:pre [(or (map? x) (set? x))]}
  (->Contains x))

(defrecord InAnyOrder [content]
  data/Diff
  (diff-similar [this b]
    (case (data/equality-partition this)
      ; FIXME: Doesn't work with inner contains
      :sequential (data/diff-similar (set content) (set b))))
  data/EqualityPartition
  (equality-partition [this]
    (clojure.data/equality-partition content)))

(defmethod print-method InAnyOrder [v ^java.io.Writer w]
  (.write w (str "(in-any-order " (pr-str (:content v)) ")")))

(defn in-any-order [x]
  {:pre [(sequential? x)]}
  (->InAnyOrder x))

(defmethod assert-expr 'same
  [msg form]
  `(let [expected# ~(nth form 1)
         value# ~(nth form 2)
         [only-expected# only-value# _both#] (data/diff expected# value#)]
     (if (and (nil? only-expected#)
              (nil? only-value#))
       (do-report {:type :pass,
                   :message ~msg
                   :expected expected#,
                   :actual value#})
       (do-report {:type :fail,
                   :message ~msg
                   :expected (list '= expected# value#),
                   :actual (list 'not (list '= expected# value#))}))))

; TODO: Extend clojure.test/report
