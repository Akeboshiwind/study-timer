create table `time` (
       `id`         int auto_increment not null,
       `user_id`    int not null,
       `time`       int not null,
       primary key (`id`),
       foreign key (`user_id`)
               references `user`(`id`)
               on delete cascade
);
