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

(defn dissoc-keys
  [m keys]
  (apply dissoc m keys))

(defn post-request
  [{:keys [uri token params on-success on-failure] :as opts}]
  (merge
   (dissoc-keys opts [:token])
   {:method          :post
    :uri             uri
    :params          params
    :timeout         5000
    :format          (ajax/json-request-format)
    :response-format (ajax/json-response-format {:keywords? true})
    :on-success      on-success
    :on-failure      on-failure}
   (when-not (nil? token)
     {:headers {"Authorization" (str "Token " token)}})))

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
  [{:keys [uri token params on-success on-failure] :as opts}]
  (merge
   (dissoc-keys opts [:uri :token :params])
   {:method :get
    :uri (str uri (make-query-params params))
    :timeout 5000
    :response-format (ajax/json-response-format {:keywords? true})
    :on-success      on-success
    :on-failure      on-failure}
   (when-not (nil? token)
     {:headers {"Authorization" (str "Token " token)}})))

(defn drop-nth
  [n coll]
  (keep-indexed #(if (not= %1 n) %2) coll))

(defn insert-nth
  [index value coll]
  (let [[before after] (split-at index coll)]
    (concat before [value] after)))
