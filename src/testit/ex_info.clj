(ns testit.ex-info
  (:import (clojure.lang ExceptionInfo)))

(defn ex-info? [message data]
  (let [message-check (cond
                        (nil? message) (constantly true)
                        (string? message) (partial = message)
                        (fn? message) message)
        data-check (cond
                     (nil? data) (constantly true)
                     (map? data) =
                     (fn? data) data)]
    (fn [e]
      (and (instance? ExceptionInfo e)
           (message-check (.getMessage ^ExceptionInfo e))
           (data-check (.getData ^ExceptionInfo e))))))
