create table `user` (
       `id`         int auto_increment not null,
       `username`   varchar(255) unique not null,
       `hash`       varchar(255) not null,
       primary key (`id`)
);
