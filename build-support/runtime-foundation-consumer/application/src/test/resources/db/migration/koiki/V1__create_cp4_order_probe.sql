create table koiki_cp4_order_probe (
    id integer primary key,
    marker varchar(100) not null
);

insert into koiki_cp4_order_probe (id, marker)
values (1, 'koiki-before-customer');
