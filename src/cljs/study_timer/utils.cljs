(ns study-timer.utils
  (:require [cljs-time.core :as t]
            [cljs-time.coerce :as tc]
            [cljs-time.format :as tf]))

(defn now
  []
  (tc/to-long (t/now)))

(defn ->clock-string
  [diff-int]
  (let [diff (tc/from-long diff-int)
        custom-format (tf/formatter "HH:mm:ss")]
    (tf/unparse custom-format diff)))
