(ns user
  (:require [luminus-migrations.core :as migrations]
            [study-timer.config :refer [env]]
            [mount.core :as mount]
            [study-timer.figwheel :refer [start-fw stop-fw cljs]]
            study-timer.core))

(defn start []
  (mount/start-without #'study-timer.core/repl-server))

(defn stop []
  (mount/stop-except #'study-timer.core/repl-server))

(defn restart []
  (stop)
  (start))

(defn migrate []
  (migrations/migrate ["migrate"] (select-keys env [:database-url])))

(defn rollback []
  (migrations/migrate ["rollback"] (select-keys env [:database-url])))


