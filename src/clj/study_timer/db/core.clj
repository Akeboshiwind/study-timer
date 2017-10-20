(ns study-timer.db.core
  (:require
    [clj-time.jdbc]
    [clojure.java.jdbc :as jdbc]
    [conman.core :as conman]
    [study-timer.config :refer [env]]
    [mount.core :refer [defstate]])
  (:import [java.sql
            BatchUpdateException
            PreparedStatement]))

(defn make-database-url
  [url port name user pass]
  (str "mysql://" url
       ":" port
       "/" name
       "?user=" user
       "&password=" pass))

(defstate ^:dynamic *db*
  :start (conman/connect! {:jdbc-url (make-database-url (env :db-url)
                                                        (env :db-port)
                                                        (env :db-name)
                                                        (env :db-user)
                                                        (env :db-pass))})
  :stop (conman/disconnect! *db*))

(conman/bind-connection *db* "sql/queries.sql")
