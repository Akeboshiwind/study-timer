(ns study-timer.env
  (:require [selmer.parser :as parser]
            [clojure.tools.logging :as log]
            [study-timer.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "\n-=[study-timer started successfully using the development profile]=-"))
   :stop
   (fn []
     (log/info "\n-=[study-timer has shut down successfully]=-"))
   :middleware wrap-dev})
