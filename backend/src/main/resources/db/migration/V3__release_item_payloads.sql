alter table release_change_item add column if not exists item_type varchar(32) not null default 'COMPONENT';
alter table release_change_item add column if not exists payload_path varchar(512) not null default '';
alter table release_change_item add column if not exists execution_content text not null default '';
alter table release_change_item add column if not exists rollback_content text not null default '';
alter table release_change_item add column if not exists code_change_summary text not null default '';
alter table release_change_item add column if not exists risk_analysis text not null default '';
alter table release_change_item add column if not exists bug_analysis text not null default '';
alter table release_change_item add column if not exists verification_commands text not null default '';
