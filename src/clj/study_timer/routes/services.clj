(ns study-timer.routes.services
  (:require [ring.util.http-response :refer :all]
            [compojure.api.sweet :refer :all]
            [schema.core :as s]
            [compojure.api.meta :refer [restructure-param]]
            [buddy.auth.accessrules :refer [restrict]]
            [buddy.auth :refer [authenticated?]]
            [study-timer.db.core :as db]
            [study-timer.utils :refer :all]
            [buddy.hashers :as h]
            [clojure.tools.logging :as log]
            [conman.core :as conman]))

(defn access-error [_ _]
  (unauthorized {:error "unauthorized"}))

(defn wrap-restricted [handler rule]
  (restrict handler {:handler  rule
                     :on-error access-error}))

(defmethod restructure-param :auth-rules
  [_ rule acc]
  (update-in acc [:middleware] conj [wrap-restricted rule]))

(defmethod restructure-param :current-user
  [_ binding acc]
  (update-in acc [:letks] into [binding `(:identity ~'+compojure-api-request+)]))

(defn login
  [username password request]
  (let [user (db/get-user-by-username {:username username})
        session (:session request)]
    (if (h/check password (:hash user))
      (let [updated-session (assoc session :identity (:id user))]
        (-> (ok {:ok true})
            (assoc :session updated-session)))
      (unauthorized {:ok false :message "Incorrect credentials"}))))

(defn logout
  [request]
  (let [session (:session request)
        updated-session (dissoc session :identity)]
    (-> (ok {:ok true})
        (assoc :session updated-session))))

(defn register-user
  "Registers a user in the database

  Returns the id if sucessful,
  nil otherwise"
  [username password]
  (when (not (db/get-user-by-username {:username username}))
    (-> {:username username
         :hash (h/derive password)}
        (db/create-user!)
        (:generated_key))))

(defn register
  [username password request]
  (if (>= (count username) 3)
    (if-let [user-id (register-user username password)]
      (let [session (:session request)
            updated-session (assoc session :identity user-id)]
        (-> (ok {:ok true})
            (assoc :session updated-session)))
      (bad-request {:ok false :message "Username already exists"}))
    (bad-request {:ok false :message "Username must be at least 3 characters long"})))

(defn unregister
  [username request]
  (let [session (:session request)
        current-user (:identity session)
        requested-user (db/get-user-by-username {:username username})]
    (if (= current-user (:id requested-user))
      (let [updated-session (dissoc session :identity)]
        (db/delete-user! {:id current-user})
        (-> (ok {:ok true})
            (assoc :session updated-session)))
      (unauthorized {:ok false :message "Not logged in"}))))

(defn add-time
  [time request]
  (let [session (:session request)]
    (if-let [current-user (:identity session)]
      (do
        (db/add-time! {:user-id current-user
                       :time time})
        (ok {:ok true}))
      (unauthorized {:ok false :message "Not logged in"}))))

(defn get-times
  [request]
  (let [session (:session request)]
    (if-let [current-user (:identity session)]
      (->> (db/get-times {:user-id current-user})
           (map :time)
           (assoc {:ok true} :data)
           (ok))
      (unauthorized {:ok false :message "Not logged in"}))))

(defn sync-times
  [times request]
  (let [session (:session request)]
    (if-let [current-user (:identity session)]
      (do
        (conman/with-transaction [db/*db*]
          (doseq [t times]
            (db/add-time! {:user-id current-user
                           :time t}))
          (get-times request)))
      (unauthorized {:ok false :message "Not logged in"}))))

(defmacro wrap-log
  [request & body]
  `(do
     (log/info (str "\n\nRequest:\n" ~request "\n"))
     (let [resp# (do ~@body)]
       (log/info (str "Response:\n" resp# "\n"))
       resp#)))

(defapi service-routes
  {:swagger {:ui "/swagger-ui"
             :spec "/swagger.json"
             :data {:info {:version "1.0.0"
                           :title "Sample API"
                           :description "Sample Services"}}}}
  (context "/api/v1" []
           (context "/user" []
                    (POST "/login" request
                          :body-params [username :- String,
                                        password :- String]
                          :summary "Do the login"
                          (login username password request))

                    (GET "/logout" request
                          :summary "Logout the current user"
                          (logout request))

                    (POST "/register" request
                          :body-params [username :- String,
                                        password :- String]
                          :summary "Register a new user, username has to be unique"
                          (register username password request))

                    (POST "/unregister" request
                          :body-params [username :- String]
                          :summary "Deletes an existing user"
                          (unregister username request)))

           (context "/time" []
                    (POST "/add" request
                          :body-params [time :- Long]
                          :summary "Add a time"
                          (add-time time request))

                    (GET "/get" request
                          :summary "Get all the times for the current user, in order"
                          (get-times request))
                    (POST "/sync" request
                          :body-params [times :- [Long]]
                          :summary "Add the given times to the database and return a list of all the times for the user."
                          (sync-times times request)))))
