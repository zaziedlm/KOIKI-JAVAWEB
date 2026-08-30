create table kkbiz_work_item_maintenance (
    task_key varchar(100) primary key,
    execution_count bigint not null default 0,
    last_execution_id uuid,
    last_executed_at timestamptz
);

insert into kkbiz_work_item_maintenance (task_key)
values ('workitem-maintenance-primary'), ('workitem-maintenance-secondary');
