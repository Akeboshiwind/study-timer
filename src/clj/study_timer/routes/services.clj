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
            [conman.core :as conman]
            [study-timer.middleware :refer [token]]
            [clj-time.core :refer [now]]))

(defn access-error [_ _]
  (unauthorized {:ok false :message "Unauthorized"}))

(defn wrap-restricted [handler rule]
  (restrict handler {:handler  rule
                     :on-error access-error}))

(defn user-exists?
  [request]
  (if-let [current-user (get-in request [:identity :user])]
    (boolean (db/get-user {:id current-user}))
    false))

(def authed?
  {:and [authenticated? user-exists?]})

(defmethod restructure-param :auth-rules
  [_ rule acc]
  (update-in acc [:middleware] conj [wrap-restricted rule]))

(defmethod restructure-param :current-user
  [_ binding acc]
  (update-in acc [:letks] into [binding `(:identity ~'+compojure-api-request+)]))

(defn login
  [req username password]
  (let [user (db/get-user-by-username {:username username})
        session (:session req)]
    (if (h/check password (:hash user))
      (let [user-id (:id user)]
        (db/set-last-login-date! {:user-id user-id
                                  :last-login-date (now)})
        (ok {:ok true
             :data {:token (token user-id)}}))
      (unauthorized {:ok false :message "Incorrect credentials"}))))

(defn refresh-token
  [req]
  (let [user-id (get-in req [:identity :user])]
    (ok {:ok true
         :data {:token (token user-id)}})))

(defn register-user
  "Registers a user in the database

  Returns the id if sucessful,
  nil otherwise"
  [username password]
  (when (not (db/get-user-by-username {:username username}))
    (-> {:username username
         :hash (h/derive password)
         :last-login-date (now)}
        (db/create-user!)
        (:generated_key))))

(defn register
  [req username password]
  (if (>= (count username) 3)
    (if-let [user-id (register-user username password)]
      (ok {:ok true
           :data {:token (token user-id)}})
      (bad-request {:ok false :message "Username already exists"}))
    (bad-request {:ok false :message "Username must be at least 3 characters long"})))

(defn unregister
  [req username]
  (let [current-user (get-in req [:identity :user])
        requested-user (db/get-user-by-username {:username username})]
    (if (= current-user (:id requested-user))
      (do
        (db/delete-user! {:id current-user})
        (ok {:ok true}))
      (unauthorized {:ok false :message "Can only unregister yourself."}))))

(defn add-time
  [req time]
  (let [current-user (get-in req [:identity :user])]
    (db/add-time! {:user-id current-user
                   :time time})
    (ok {:ok true})))

(defn delete-time
  [req index]
  (let [current-user (get-in req [:identity :user])]
    (if (zero? (db/delete-time! {:user-id current-user
                                 :index index}))
      (bad-request {:ok false :message "Index out of range"})
      (ok {:ok true}))))

(defn get-times
  [req]
  (let [current-user (get-in req [:identity :user])]
    (->> (db/get-times {:user-id current-user})
         (map :time)
         (assoc {:ok true} :data)
         (ok))))

(defn sync-times
  [req times]
  (let [current-user (get-in req [:identity :user])]
    (conman/with-transaction [db/*db*]
      (doseq [t times]
        (db/add-time! {:user-id current-user
                       :time t}))
      (get-times req))))

(defmacro wrap-log
  [req & body]
  `(do
     (log/info (str "\n\nRequest:\n" ~req "\n"))
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
           (context "/auth" []
                    (POST "/login" req
                          :body-params [username :- String,
                                        password :- String]
                          :summary "If the `username` and `password` are valid, then issue a JWE token."
                          (login req username password))

                    (GET "/refresh" req
                         :auth-rules authed?
                         :header-params [authorization :- String]
                         :summary "Refresh the given JWT if it hasn't expiered."
                         (refresh-token req)))

           (context "/user" []
                    (POST "/register" req
                          :body-params [username :- String,
                                        password :- String]
                          :summary "Register a new user, username has to be unique"
                          (register req username password))

                    (POST "/unregister" req
                          :auth-rules authed?
                          :header-params [authorization :- String]
                          :body-params [username :- String]
                          :summary "Deletes an existing user"
                          (unregister req username)))

           (context "" []
                    :auth-rules authed?
                    :header-params [authorization :- String]
                    (context "/time" []
                             (POST "/add" req
                                   :body-params [time :- Long]
                                   :summary "Add a time"
                                   (add-time req time))

                             (POST "/delete" req
                                   :body-params [index :- Long]
                                   :summary "Delete the time at the given index"
                                   (delete-time req index))

                             (GET "/get" req
                                  :summary "Get all the times for the current user, in order"
                                  (get-times req))

                             (POST "/sync" req
                                   :body-params [times :- [Long]]
                                   :summary "Add the given times to the database and return a list of all the times for the user."
                                   (sync-times req times))))))
