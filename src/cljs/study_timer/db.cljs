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

(s/def ::min-study-length int?)

(s/def ::study-log (s/coll-of int?))

(s/def ::panel
  #{:login    ;; The login page
    :register ;; The registration page
    :clock})  ;; The clock page

(s/def ::error-type
  #{:login-failure
    :get-times-failure
    :add-time-failure
    :register-password-mismatch
    :remove-time-failure
    :refresh-failure})

(s/def ::current-panel ::panel)

(s/def ::error
  (s/or :nil nil?
        :details (s/cat :type ::error-type
                        :message string?)))

(s/def ::token (s/or :nil? nil?
                     :token string?))

(s/def ::db (s/keys :req-un [::clock-state
                             ::time
                             ::display-time
                             ::break-length
                             ::min-study-length
                             ::study-log
                             ::current-panel
                             ::error
                             ::token]))

(def default-db
  {:clock-state :stopped
   :time (u/now)
   :display-time 0
   :break-length (* 5 60 1000) ;; 5 mins
   :min-study-length (* 30 1000) ;; 30 secs
   :study-log []
   :current-panel :login
   :error nil
   :token nil})
