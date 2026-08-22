-- 后端安全边界与可验收收口阶段：MySQL 增量脚本
-- 执行后，文档详情可以展示重试次数和最近一次处理起止时间；脚本可重复执行。

drop procedure if exists add_chat_column_if_missing;

delimiter $$
create procedure add_chat_column_if_missing(
    in target_table varchar(64),
    in target_column varchar(64),
    in column_definition text
)
begin
    if not exists (
        select 1
        from information_schema.columns
        where table_schema = database()
          and table_name = target_table
          and column_name = target_column
    ) then
        set @column_sql = concat('alter table `', target_table, '` add column ', column_definition);
        prepare add_column_statement from @column_sql;
        execute add_column_statement;
        deallocate prepare add_column_statement;
    end if;
end$$
delimiter ;

call add_chat_column_if_missing('knowledge_document', 'retry_count',
    '`retry_count` int not null default 0 comment ''人工重试和中断恢复次数'' after `error_message`');
call add_chat_column_if_missing('knowledge_document', 'processing_started_at',
    '`processing_started_at` datetime null comment ''最近一次处理开始时间'' after `retry_count`');
call add_chat_column_if_missing('knowledge_document', 'processing_finished_at',
    '`processing_finished_at` datetime null comment ''最近一次处理结束时间'' after `processing_started_at`');

call add_chat_column_if_missing('rag_session_document', 'retry_count',
    '`retry_count` int not null default 0 comment ''人工重试和中断恢复次数'' after `error_message`');
call add_chat_column_if_missing('rag_session_document', 'processing_started_at',
    '`processing_started_at` datetime null comment ''最近一次处理开始时间'' after `retry_count`');
call add_chat_column_if_missing('rag_session_document', 'processing_finished_at',
    '`processing_finished_at` datetime null comment ''最近一次处理结束时间'' after `processing_started_at`');

drop procedure add_chat_column_if_missing;
