(ns study-timer.views
  (:require [re-frame.core :refer [subscribe dispatch]]
            [cljs-time.core :as t]
            [cljs-time.coerce :as t.coerce]
            [cljs-time.format :as t.format]
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
  [data on-click]
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
                               (reset! chart (js/Chart. ctx (clj->js (line-chart-data data))))
                               (set! (.-onclick canvas) (on-click chart))))
      :component-did-update (fn [comp new-args]
                              (let [[_ data] (r/argv comp)
                                    chart @chart
                                    chart-data (.-data chart)
                                    datasets (aget chart-data "datasets")]
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
  [line-graph
   [{:label "Study data"
     :data logs
     :borderColor ["rgba(102,136,173,1)"]}]
   (fn [_] (fn [_]))
   #_
   (fn [chart]
     (fn [e]
       (try
         (let [chart @chart
               point (first (.getElementsAtEvent chart e))
               index (.-_index point)]
           (dispatch [:remove-time index]))
         (catch js/TypeError e nil))))])

(defn login-failure
  [message]
  [:div.error-flash
   (str message " ")
   [:a
    {:on-click #(dispatch [:set-current-panel :login])}
    "LOGIN"]
   " or "
   [:a
    {:on-click #(dispatch [:set-current-panel :register])}
    "REGISTER"]])

(defn error-display
  [details]
  (when-not (nil? details)
    (let [[type message] details]
      (condp = type
        :login-failure [login-failure message]
        [:div.error (str message)]))))

(defn clock-panel
  []
  (let [state (subscribe [:clock-state])
        time (subscribe [:clock])
        logs (subscribe [:study-log])
        error (subscribe [:error])]
    (fn []
      [:div.panel
       [error-display @error]
       [:div
        {:on-click (fn []
                     (dispatch [:logout]))}
        "LOGOUT"]
       [clock @time]
       [buttons @state]
       [study-log-chart @logs]])))

(defn input-value
  [id]
  (-> js/document
      (.getElementById id)
      (.-value)))

(defn login-panel
  []
  (let [error (subscribe [:error])]
    (fn []
      [:div.login-form
       [error-display @error]
       [:input#username.input
        {:placeholder "USERNAME"
         :type "text"}]
       [:input#password.input
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
        "LOGIN"]
       [:div
        "OR"
        [:div.button
         {:on-click (fn []
                      (dispatch [:set-current-panel :register]))}
         "REGISTER"]]])))

(defn registration-panel
  []
  (let [error (subscribe [:error])]
    (fn []
      [:div.login-form
       [error-display @error]
       [:input#username.input
        {:placeholder "USERNAME"
         :type "text"}]
       [:input#password.input
        {:placeholder "PASSWORD"
         :type "password"}]
       [:input#password2.input
        {:placeholder "RE-ENTER PASSWORD"
         :type "password"
         :on-key-press (fn [e]
                         (when (= 13 (.-charCode e))
                           (.. js/document
                               (getElementById "register-button")
                               (click))))}]
       [:div#register-button.button
        {:on-click (fn []
                     (let [username (input-value "username")
                           password (input-value "password")
                           password2 (input-value "password2")]
                       (if (= password password2)
                         (dispatch [:register username password])
                         (dispatch [:error :register-password-mismatch "Passwords don't match"]))))}
        "REGISTER"]
       [:div
        "OR"
        [:div.button
         {:on-click (fn []
                      (dispatch [:set-current-panel :login]))}
         "LOGIN"]]])))

(defn flash
  []
  (let [flash-type (subscribe [:flash-type])
        flash (subscribe [:flash])]
    (fn []
      [:div.flash "hi"])))

(defn main-panel
  []
  (let [current (subscribe [:current-panel])]
    (fn []
      [:div.content
       [flash]
       (condp = @current
         :login [login-panel]
         :register [registration-panel]
         :clock [clock-panel])])))
