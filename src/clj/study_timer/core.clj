(ns study-timer.core
  (:require [study-timer.handler :as handler]
            [luminus.repl-server :as repl]
            [luminus.http-server :as http]
            [luminus-migrations.core :as migrations]
            [study-timer.config :refer [env]]
            [clojure.tools.cli :refer [parse-opts]]
            [clojure.tools.logging :as log]
            [mount.core :as mount])
  (:gen-class))

(def cli-options
  [["-p" "--port PORT" "Port number"
    :parse-fn #(Integer/parseInt %)]])

(mount/defstate ^{:on-reload :noop}
                http-server
                :start
                (http/start
                  (-> env
                      (assoc :handler (handler/app))
                      (update :port #(or (-> env :options :port) %))))
                :stop
                (http/stop http-server))

(mount/defstate ^{:on-reload :noop}
                repl-server
                :start
                (when-let [nrepl-port (env :nrepl-port)]
                  (repl/start {:port nrepl-port}))
                :stop
                (when repl-server
                  (repl/stop repl-server)))


(defn stop-app []
  (doseq [component (:stopped (mount/stop))]
    (log/info component "stopped"))
  (shutdown-agents))

(defn start-app [args]
  (doseq [component (-> args
                        (parse-opts cli-options)
                        mount/start-with-args
                        :started)]
    (log/info component "started"))
  (.addShutdownHook (Runtime/getRuntime) (Thread. stop-app)))

(defn make-database-url
  [{:keys [db-url db-port db-name db-user db-pass]}]
  (str "mysql://" db-url
       ":" db-port
       "/" db-name
       "?user=" db-user
       "&password=" db-pass))

(defn -main [& args]
  (cond
    (some #{"init"} args)
    (do
      (mount/start #'study-timer.config/env)
      (migrations/init (merge
                        (select-keys env [:init-script])
                        {:database-url (make-database-url env)}))
      (System/exit 0))
    (some #{"migrate" "rollback"} args)
    (do
      (mount/start #'study-timer.config/env)
      (migrations/migrate args {:database-url (make-database-url env)})
      (System/exit 0))
    :else
    (start-app args)))
