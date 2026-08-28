create table kkbiz_independent_probe (
    id integer primary key,
    marker varchar(32) not null
);

insert into kkbiz_independent_probe (id, marker) values (1, 'customer-v1');
