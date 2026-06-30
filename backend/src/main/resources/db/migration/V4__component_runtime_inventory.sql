alter table component add column if not exists action_types varchar(512) not null default '';
alter table component add column if not exists observed_status varchar(64) not null default 'UNKNOWN';
alter table component add column if not exists runtime_inventory text not null default '';
