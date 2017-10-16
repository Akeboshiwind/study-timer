(ns study-timer.core
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [study-timer.events]
            [study-timer.subs]
            [study-timer.views :as views]
            [study-timer.config :as config]))

(defn mount-root
  []
  (rf/clear-subscription-cache!)
  (r/render [views/main-panel]
            (.getElementById js/document "app")))

(defn ^:export init!
  []
  (rf/dispatch-sync [:initialize-db])
  (mount-root))
