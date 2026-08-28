create table kkbiz_order_probe as
select id, marker
from koiki_order_probe;

alter table kkbiz_order_probe add primary key (id);
