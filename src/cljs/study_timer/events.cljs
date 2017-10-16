(ns study-timer.events
  (:require [re-frame.core :as rf]
            [study-timer.db :as db]
            [cljs.spec.alpha :as s]
            [study-timer.utils :as u]
            [study-timer.effects :as e]))

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
      (if (= :study (:clock-state db))
        {:dispatch [:add-study-time (:display-time db)]})))))

(rf/reg-event-db
 :add-study-time
 [check-spec-interceptor
  rf/trim-v]
 (fn [db [time]]
   (let [log (:study-log db)]
     (assoc db :study-log (conj log time)))))

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
   (assoc db :current-panel new-panel)))

(rf/reg-event-fx
 :login
 [check-spec-interceptor
  rf/trim-v]
 (fn [cofx [username password]]
   (merge {}
          (if (and (= username "ake")
                   (= password "password"))
            {:dispatch [:set-current-panel :clock]}
            {:dispatch [:error :login "Incorrect login details"]}))))

(rf/reg-event-db
 :error
 [check-spec-interceptor
  rf/trim-v]
 (fn [db details]
   (assoc db :error details)))
