create table koiki_independent_probe (
    id integer primary key,
    marker varchar(32) not null
);

insert into koiki_independent_probe (id, marker) values (1, 'koiki-v1');
