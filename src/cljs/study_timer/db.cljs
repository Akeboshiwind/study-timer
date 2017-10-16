(ns study-timer.db
  (:require [cljs.spec.alpha :as s]
            [study-timer.utils :as u]))

;; Which clock is running?
(s/def ::clock-state
  #{:stopped  ;; Clocks are stopped
    :study    ;; Study clock is running
    :break})  ;; Break clock is running

(s/def ::time int?)

(s/def ::display-time int?)

(s/def ::break-length int?)

(s/def ::study-log (s/coll-of int?))

(s/def ::panel
  #{:login    ;; The login page
    :clock})  ;; The clock page

(s/def ::current-panel ::panel)

(s/def ::error
  (s/or :nil nil?
        :details (s/cat :panel ::panel
                        :message string?)))

(s/def ::db (s/keys :req-un [::clock-state
                             ::time
                             ::display-time
                             ::break-length
                             ::study-log
                             ::current-panel
                             ::error]))

(def default-db
  {:clock-state :stopped
   :time (u/now)
   :display-time 0
   :break-length (* 5 60 1000) ;; 5 mins
   :study-log []
   :current-panel :login
   :error nil})
