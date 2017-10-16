-- :name create-user! :! :n
-- :doc creates a new user record
insert
into `user`
(username, hash)
values (:username, :hash)

-- :name update-user! :! :n
-- :doc update an existing user record
update `user`
set username = :username, hash = :hash
where id = :id

-- :name get-user :? :1
-- :doc retrieve a user given the username
select *
from `user`
where username = username

-- :name delete-user! :! :n
-- :doc delete a user given the id
delete
from `user`
where id = :id

-- :name add-time! :! :n
-- :doc creates a new time entry for a user
insert
into `time`
(user_id, `time`)
values (:user-id, :time)

-- :name get-times :? :*
-- :doc retrieve a list of times for a user
select `time`
from `time`
where user_id = :user-id
