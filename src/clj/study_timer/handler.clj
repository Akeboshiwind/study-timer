(ns study-timer.handler
  (:require [compojure.core :refer [routes wrap-routes]]
            [study-timer.layout :refer [error-page]]
            [study-timer.routes.home :refer [home-routes]]
            [study-timer.routes.services :refer [service-routes]]
            [compojure.route :as route]
            [study-timer.env :refer [defaults]]
            [mount.core :as mount]
            [study-timer.middleware :as middleware]))

(mount/defstate init-app
                :start ((or (:init defaults) identity))
                :stop  ((or (:stop defaults) identity)))

(def app-routes
  (routes
    (-> #'home-routes
        (wrap-routes middleware/wrap-csrf)
        (wrap-routes middleware/wrap-formats))
    #'service-routes
    (route/not-found
      (:body
        (error-page {:status 404
                     :title "page not found"})))))


(defn app [] (middleware/wrap-base #'app-routes))
