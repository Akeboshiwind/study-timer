-- :name create-user! :i!
-- :doc creates a new user record
insert
into `user`
(username, hash, last_login_date)
values (:username, :hash, :last-login-date)

-- :name update-user! :! :n
-- :doc update an existing user record
update `user`
set username = :username, hash = :hash
where id = :id

-- :name get-user :? :1
-- :doc retrieve a user given the id
select *
from `user`
where id = :id

-- :name get-user-by-username :? :1
-- :doc retrieve a user given the username
select *
from `user`
where username = :username

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
order by id

-- :name delete-time! :! :n
-- :doc delete the time at the given index
delete t.*
from `time` as t
inner join (select id
              from `time`
              where user_id = :user-id
              order by id
              limit 1
              offset :index) as t2
        on t.id = t2.id

-- :name set-last-login-date! :! :n
update `user`
set last_login_date = :last-login-date
where id = :user-id

-- :name logout! :! :n
update `user`
set last_login_date = null
where id = :user-id
