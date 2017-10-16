(ns study-timer.views
  (:require [re-frame.core :refer [subscribe dispatch]]
            [cljs-time.core :as t]
            [cljs-time.coerce :as t.coerce]
            [cljs-time.format :as t.format]
            [cljs.core.async.impl.dispatch :as dispatch]
            [reagent.core :as r]
            [cljsjs.chartjs]
            [study-timer.utils :as u]))

(defn dispatch-tick-event
  []
  (dispatch [:tick]))

(defonce tick-timer (js/setInterval dispatch-tick-event 1000))

(defn clock
  [clock]
  [:div.clock
   [:p clock]])

(defn state-button
  [text new-state]
  [:div.button
   (merge {:on-click #(dispatch [:set-clock-state new-state])}
          (when (= :stopped new-state)
            {:class "stop"}))
   text])

(defn buttons
  [state]
  [:div.buttons
   (if (= :study state)
     [state-button "BREAK" :break]
     [state-button "START" :study])
   (if (not (= :stopped state))
     [state-button "STOP" :stopped])])

(defn line-chart-data
  [data]
  {:type "line"
   :data {:labels (-> data
                      (first)
                      (:data)
                      (count)
                      (repeat ""))
          :datasets data}
   :options {:scales {:yAxes [{:ticks {:callback (fn [tick, _, _] (u/->clock-string tick))}}]}}})

(defn line-graph
  [data]
  (let [id (str (random-uuid))
        chart (atom nil)]
    (r/create-class
     {:display-name "line-graph"
      :reagent-render (fn []
                        [:canvas {:id id}])
      :component-did-mount (fn [comp]
                             (let [canvas (.getElementById js/document id)
                                   ctx (.getContext canvas "2d")]
                               (set! (.-height canvas) 162)
                               (reset! chart (js/Chart. ctx (clj->js (line-chart-data data))))))
      :component-did-update (fn [comp new-args]
                              (let [[_ data] (r/argv comp)
                                    chart @chart
                                    chart-data (.-data chart)
                                    datasets (.-datasets chart-data)]
                                (set! (.-labels chart-data)
                                      (-> data
                                          (first)
                                          (:data)
                                          (count)
                                          (repeat "")
                                          (clj->js)))
                                (doall
                                 (for [[i dataset] (zipmap (range) datasets)]
                                   (set! (.-data dataset)
                                         (-> data
                                             (nth i)
                                             (:data)
                                             (clj->js)))))
                                (.update chart)))})))

(defn study-log-chart
  [logs]
  [line-graph [{:label "Study data"
                :data logs
                :borderColor ["rgba(102,136,173,1)"]}]])

(defn clock-panel
  []
  (let [state (subscribe [:clock-state])
        time (subscribe [:clock])
        logs (subscribe [:study-log])]
    (fn []
      [:div.panel
       [clock @time]
       [buttons @state]
       [study-log-chart @logs]])))

(defn input-value
  [id]
  (-> js/document
      (.getElementById id)
      (.-value)))

(defn form-input
  [id type])

(defn error-display
  [[panel message]]
  [:div.error
   message])

(defn login-panel
  [error]
  [:div.login-form
   [error-display error]
   [:input#username
    {:placeholder "USERNAME"
     :type "text"}]
   [:input#password
    {:placeholder "PASSWORD"
     :type "password"
     :on-key-press (fn [e]
                     (when (= 13 (.-charCode e))
                       (.. js/document
                           (getElementById "login-button")
                           (click))))}]
   [:div#login-button.button
    {:on-click (fn []
                 (let [username (input-value "username")
                       password (input-value "password")]
                   (dispatch [:login username password])))}
    "LOGIN"]])

(defn main-panel
  []
  (let [current (subscribe [:current-panel])
        error (subscribe [:error])]
    (fn []
      (condp = @current
        :login [login-panel @error]
        :clock [clock-panel @error]))))
