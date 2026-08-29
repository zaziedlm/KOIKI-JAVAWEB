create table kkbiz_work_review (
    work_item_id uuid primary key references kkbiz_work_item(id),
    label varchar(100) not null,
    status varchar(20) not null,
    version bigint not null
);
