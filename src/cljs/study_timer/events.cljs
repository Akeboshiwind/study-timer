(ns study-timer.events
  (:require [re-frame.core :as rf]
            [study-timer.db :as db]
            [cljs.spec.alpha :as s]
            [study-timer.utils :as u]
            [study-timer.effects :as e]
            [day8.re-frame.http-fx]
            [ajax.core :as ajax]
            [expound.alpha :as expound]))

(defn check-and-throw
  "Throws an exception if `db` doesn't match the Spec `a-spec`."
  [a-spec db]
  (when-not (s/valid? a-spec db)
    (throw (ex-info (str "spec check failed: " (expound/expound-str a-spec db)) {}))))

(def check-spec-interceptor (rf/after (partial check-and-throw :study-timer.db/db)))

(rf/reg-cofx
 :now
 (fn [coeffects _]
   (assoc coeffects :now (u/now))))

(rf/reg-event-db
 :initialize-db
 [check-spec-interceptor]
 (fn  [_ _]
   db/default-db))

(rf/reg-event-fx
 :set-clock-state
 [check-spec-interceptor
  (rf/inject-cofx :now)
  rf/trim-v]
 (fn [cofx [new-state]]
   (let [now (:now cofx)
         db (:db cofx)
         time (if (= :break new-state)
                (+ now (:break-length db))
                now)]
     (merge
      {:db (assoc db :clock-state new-state
                  :time time
                  :display-time 0)}
      (when (= :study (:clock-state db))
        {:dispatch [:add-study-time (:display-time db)]})))))

(rf/reg-event-fx
 :add-study-time
 [check-spec-interceptor
  rf/trim-v]
 (fn [cofx [time]]
   (let [db (:db cofx)
         token (:token db)
         log (:study-log db)
         min-study-length (:min-study-length db)]
     (when (> time min-study-length)
       (merge {:db (assoc db :study-log (conj log time))}
              (when-not (nil? (:token db))
                {:dispatch [:error nil]
                 :http-xhrio (u/post-request {:uri "/api/v1/time/add"
                                              :token token
                                              :params {:time time}
                                              :on-success [:none]
                                              :on-failure [:add-time-failure]})}))))))

(rf/reg-event-fx
 :add-time-failure
 [check-spec-interceptor
  rf/trim-v]
 (fn [cofx [{:keys [response]}]]
   {:dispatch [:error :add-time-failure (:message response)]}))

(rf/reg-event-fx
 :tick
 [check-spec-interceptor
  (rf/inject-cofx :now)]
 (fn [cofx _]
   (let [now (:now cofx)
         db (:db cofx)
         start-time (:time db)
         display-time (case (:clock-state db)
                        :stopped 0
                        :study (- now start-time)
                        :break (max 0 (- start-time now)))]
     (merge
      {:db (assoc db :display-time display-time)}
      (when (and (= :break (:clock-state db))
                 (>= 0 display-time))
        {:beep {}})))))

(rf/reg-event-db
 :set-current-panel
 [check-spec-interceptor
  rf/trim-v]
 (fn [db [new-panel]]
   (assoc db :current-panel new-panel
          :clock-state :stopped)))

(rf/reg-event-fx
 :login
 [check-spec-interceptor
  rf/trim-v]
 (fn [cofx [username password]]
   {:dispatch-n [[:set-current-panel :clock]
                 [:error nil]]
    :http-xhrio (u/post-request {:uri "/api/v1/auth/login"
                                 :params {:username username
                                          :password password}
                                 :on-success [:login-success]
                                 :on-failure [:login-failure]})}))

(rf/reg-event-fx
 :login-success
 [check-spec-interceptor
  rf/trim-v]
 (fn [cofx [response]]
   (let [db (:db cofx)
         token (get-in response [:data :token])]
     {:db (assoc db
                 :token token)
      :dispatch-later [{:ms (* 1000 60 55) ;; 55 mins
                        :dispatch [:refresh-token]}]
      :http-xhrio (u/post-request {:uri  "/api/v1/time/sync"
                                   :token token
                                   :params {:times (:study-log db)}
                                   :on-success [:time-sync-success]
                                   :on-failure [:time-sync-failure]})})))

(rf/reg-event-fx
 :login-failure
 [check-spec-interceptor
  rf/trim-v]
 (fn [cofx [{:keys [response]}]]
   {:dispatch-n [[:error :login-failure (:message response)]
                 [:set-current-panel :login]]}))

(rf/reg-event-fx
 :refresh-token
 [check-spec-interceptor
  rf/trim-v]
 (fn [cofx _]
   (let [db (:db cofx)
         token (:token db)]
     {:http-xhrio (u/get-request {:uri "/api/v1/auth/refresh"
                                  :token token
                                  :on-success [:refresh-success]
                                  :on-failure [:refresh-failure]})})))

(rf/reg-event-fx
 :refresh-success
 [check-spec-interceptor
  rf/trim-v]
 (fn [cofx [response]]
   (let [db (:db cofx)]
     {:db (assoc db :token (get-in response [:data :token]))
      :dispatch-later [{:ms (* 1000 60 55) ;; 55 mins
                        :dispatch [:refresh-token]}]})))

(rf/reg-event-fx
 :refresh-failure
 [check-spec-interceptor
  rf/trim-v]
 (fn [cofx [{:keys [response]}]]
   {:dispatch [:error :refresh-failure (:message response)]}))

(rf/reg-event-db
 :time-sync-success
 [check-spec-interceptor
  rf/trim-v]
 (fn [db [response]]
   (let [times (:data response)]
     (assoc db :study-log times))))

(rf/reg-event-fx
 :time-sync-failure
 [check-spec-interceptor
  rf/trim-v]
 (fn [cofx [{:keys [response]}]]
   {:dispatch [:error :get-times-failure (:message response)]}))

;; Todo: look into possible race condition on login event when setting error
(rf/reg-event-db
 :error
 [check-spec-interceptor
  rf/trim-v]
 (fn [db details]
   (assoc db :error details)))

(rf/reg-event-db
 :set-token
 [check-spec-interceptor
  rf/trim-v]
 (fn [db [value & _]]
   (assoc db :token value)))

(rf/reg-event-fx
 :register
 [check-spec-interceptor
  rf/trim-v]
 (fn [cofx [username password]]
   (let [db (:db cofx)
         token (:token db)]
     {:dispatch-n [[:set-current-panel :clock]
                   [:error nil]]
      :http-xhrio (u/post-request {:uri "/api/v1/user/register"
                                   :token token
                                   :params {:username username :password password}
                                   :on-success [:login-success]
                                   :on-failure [:register-failure]})})))

(rf/reg-event-fx
 :register-failure
 [check-spec-interceptor
  rf/trim-v]
 (fn [cofx [{:keys [response]}]]
   {:dispatch-n [[:error :login-failure (:message response)]
                 [:set-current-panel :register]]}))

(rf/reg-event-fx
 :logout
 [check-spec-interceptor
  rf/trim-v]
 (fn [cofx _]
   (let [db (:db cofx)
         token (:token db)]
     {:dispatch [:initialize-db]
      :http-xhrio (u/get-request {:uri "/api/v1/user/logout"
                                  :token token
                                  :on-success [:none]
                                  :on-failure [:none]})})))

(rf/reg-event-db
 :none
 [check-spec-interceptor
  rf/trim-v]
 (fn [db _]
   db))

(rf/reg-event-fx
 :remove-time
 [check-spec-interceptor
  rf/trim-v]
 (fn [cofx [index]]
   (let [db (:db cofx)
         token (:token db)
         study-log (:study-log db)]
     {:db (assoc db :study-log (u/drop-nth index study-log))
      :http-xhrio (u/post-request {:uri "/api/v1/time/delete"
                                   :token token
                                   :params {:index index}
                                   :on-success [:none]
                                   :on-failure [:remove-time-failure index (nth study-log index)]})})))

(rf/reg-event-fx
 :remove-time-failure
 [check-spec-interceptor
  rf/trim-v]
 (fn [cofx [index value {:keys [response]}]]
   (let [db (:db cofx)
         study-log (:study-log db)]
     {:db (assoc db :study-log (u/insert-nth index value study-log))
      :dispatch [:error :remove-time-failure (:message response)]})))
