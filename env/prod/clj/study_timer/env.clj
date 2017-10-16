(ns study-timer.env
  (:require [clojure.tools.logging :as log]))

(def defaults
  {:init
   (fn []
     (log/info "\n-=[study-timer started successfully]=-"))
   :stop
   (fn []
     (log/info "\n-=[study-timer has shut down successfully]=-"))
   :middleware identity})
