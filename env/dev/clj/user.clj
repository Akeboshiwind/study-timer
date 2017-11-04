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

(defn make-database-url
  [{:keys [db-url db-port db-name db-user db-pass]}]
  (str "mysql://" db-url
       ":" db-port
       "/" db-name
       "?user=" db-user
       "&password=" db-pass))

(defn migrate []
  (migrations/migrate ["migrate"] {:database-url (make-database-url env)}))

(defn rollback []
  (migrations/migrate ["rollback"] {:database-url (make-database-url env)}))
