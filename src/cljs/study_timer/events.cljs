(ns study-timer.events
  (:require [re-frame.core :as rf]
            [study-timer.db :as db]
            [cljs.spec.alpha :as s]
            [study-timer.utils :as u]
            [study-timer.effects :as e]
            [day8.re-frame.http-fx]
            [ajax.core :as ajax]))

(defn check-and-throw
  "Throws an exception if `db` doesn't match the Spec `a-spec`."
  [a-spec db]
  (when-not (s/valid? a-spec db)
    (throw (ex-info (str "spec check failed: " (s/explain-str a-spec db)) {}))))

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
         log (:study-log db)
         min-study-length (:min-study-length db)]
     (when (> time min-study-length)
       (merge {:db (assoc db :study-log (conj log time))}
              (when (:logged-in? db)
                {:http-xhrio (u/post-request "/api/v1/time/add"
                                             {:time time}
                                             [:add-time-success]
                                             [:add-time-failure])}))))))

(rf/reg-event-db
 :add-time-success
 [check-spec-interceptor
  rf/trim-v]
 (fn [db [{:keys [response]}]]
   (assoc db :error nil)))

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
          :error nil
          :clock-state :stopped)))

(rf/reg-event-fx
 :login
 [check-spec-interceptor
  rf/trim-v]
 (fn [cofx [username password]]
   (let [db (:db cofx)]
     {:dispatch [:set-current-panel :clock]
      :http-xhrio (u/post-request "/api/v1/user/login"
                                  {:username username :password password}
                                  [:login-success]
                                  [:login-failure])})))

(rf/reg-event-fx
 :login-success
 [check-spec-interceptor
  rf/trim-v]
 (fn [cofx _]
   (let [db (:db cofx)]
     {:db (assoc db :error nil)
      :dispatch [:set-logged-in true]
      :http-xhrio (u/post-request "/api/v1/time/sync"
                                  {:times (:study-log db)}
                                  [:time-sync-success]
                                  [:time-sync-failure])})))

(rf/reg-event-fx
 :login-failure
 [check-spec-interceptor
  rf/trim-v]
 (fn [cofx [{:keys [response]}]]
   {:dispatch [:error :login-failure (:message response)]}))

(rf/reg-event-db
 :time-sync-success
 [check-spec-interceptor
  rf/trim-v]
 (fn [db [response]]
   (let [times (:data response)]
     (assoc db :study-log times
               :error nil))))

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
 :set-logged-in
 [check-spec-interceptor
  rf/trim-v]
 (fn [db [value & _]]
   (assoc db :logged-in? value)))

(rf/reg-event-fx
 :register
 [check-spec-interceptor
  rf/trim-v]
 (fn [cofx [username password]]
   (let [db (:db cofx)]
     {:dispatch [:set-current-panel :clock]
      :http-xhrio (u/post-request "/api/v1/user/register"
                                  {:username username :password password}
                                  [:login-success]
                                  [:login-failure])})))

(rf/reg-event-fx
 :logout
 [check-spec-interceptor
  rf/trim-v]
 (fn [cofx _]
   {:dispatch [:initialize-db]
    :http-xhrio (u/get-request "/api/v1/user/logout"
                               {}
                               [:none]
                               [:none])}))

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
         study-log (:study-log db)]
     {:db (assoc db :study-log (u/drop-nth index study-log))
      :http-xhrio (u/post-request "/api/v1/time/delete"
                                  {:index index}
                                  [:none]
                                  [:remove-time-failure index (nth study-log index)])})))

(rf/reg-event-fx
 :remove-time-failure
 [check-spec-interceptor
  rf/trim-v]
 (fn [cofx [index value {:keys [response]}]]
   (let [db (:db cofx)
         study-log (:study-log db)]
     {:db (assoc db :study-log (u/insert-nth index value study-log))
      :dispatch [:error :remove-time-failure (:message response)]})))
