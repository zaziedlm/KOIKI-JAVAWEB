create table kkbiz_migration_order_probe (
    id integer primary key,
    marker varchar(100) not null
);

insert into kkbiz_migration_order_probe (id, marker)
select id, marker
from koiki_cp4_order_probe;
