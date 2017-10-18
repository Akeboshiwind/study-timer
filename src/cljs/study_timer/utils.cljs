(ns study-timer.utils
  (:require [cljs-time.core :as t]
            [cljs-time.coerce :as tc]
            [cljs-time.format :as tf]
            [ajax.core :as ajax]
            [clojure.string :as s]))

(defn now
  []
  (tc/to-long (t/now)))

(defn ->clock-string
  [diff-int]
  (let [diff (tc/from-long diff-int)
        custom-format (tf/formatter "HH:mm:ss")]
    (tf/unparse custom-format diff)))

(defn post-request
  [uri params on-success on-failure]
  {:method          :post
   :uri             uri
   :params          params
   :timeout         5000
   :format          (ajax/json-request-format)
   :response-format (ajax/json-response-format {:keywords? true})
   :on-success      on-success
   :on-failure      on-failure})

(defn make-query-params
  [params]
  (loop [acc []
         params params]
    (if (empty? params)
      (str "?" (s/join "&" acc))
      (let [param (first params)
            key (name (first param))
            value (second param)]
        (recur (conj acc (str key "=" value))
               (rest params))))))

(defn get-request
  [uri params on-success on-failure]
  {:method :get
   :uri (str uri (make-query-params params))
   :timeout 5000
   :response-format (ajax/json-response-format {:keywords? true})
   :on-success      on-success
   :on-failure      on-failure})
