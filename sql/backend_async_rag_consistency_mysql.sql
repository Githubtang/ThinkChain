-- 后端文档异步处理与 RAG 一致性阶段：MySQL 增量脚本
-- 作用：加速“按会话读取最近 N 条消息”的历史恢复查询。
-- 本脚本可重复执行；索引已存在时不会再次创建。

set @index_exists = (
    select count(1)
    from information_schema.statistics
    where table_schema = database()
      and table_name = 'chat_message'
      and index_name = 'idx_chat_message_conversation_time'
);

set @create_index_sql = if(
    @index_exists = 0,
    'create index idx_chat_message_conversation_time on chat_message (conversation_id, create_time, id)',
    'select 1'
);

prepare create_index_statement from @create_index_sql;
execute create_index_statement;
deallocate prepare create_index_statement;
