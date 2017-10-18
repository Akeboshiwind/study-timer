(ns study-timer.subs
  (:require [re-frame.core :refer [reg-sub]]
            [study-timer.utils :as u]))

(reg-sub
 :clock-state
 (fn [db]
   (:clock-state db)))

(reg-sub
 :time
 (fn [db]
   (:display-time db)))

(reg-sub
 :study-log
 (fn [db]
   (:study-log db)))

(reg-sub
 :clock
 :<- [:time]
 (fn [time _]
   (u/->clock-string time)))

(reg-sub
 :current-panel
 (fn [db]
   (:current-panel db)))

(reg-sub
 :error
 (fn [db]
   (:error db)))
