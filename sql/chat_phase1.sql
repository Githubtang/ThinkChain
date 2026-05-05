-- ThinkChain Chat phase 1 tables

create table if not exists chat_conversation (
    id varchar(64) not null comment '会话ID',
    user_id varchar(64) null comment '所属用户ID',
    title varchar(200) null comment '会话标题',
    model varchar(100) not null comment '逻辑模型名称',
    system_prompt text null comment '会话使用的系统提示词',
    create_time datetime null comment '创建时间',
    update_time datetime null comment '更新时间',
    primary key (id),
    index idx_chat_conversation_user_id (user_id),
    index idx_chat_conversation_update_time (update_time)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_unicode_ci comment='AI聊天会话表';

create table if not exists chat_message (
    id varchar(64) not null comment '消息ID',
    conversation_id varchar(64) not null comment '会话ID',
    role varchar(32) not null comment '消息角色：system、user、assistant',
    content_type varchar(32) null comment '主要内容类型',
    content text null comment '扁平化文本内容',
    raw_content longtext null comment '原始多模态内容或厂商响应',
    model varchar(100) null comment '逻辑模型名称',
    create_time datetime null comment '创建时间',
    primary key (id),
    index idx_chat_message_conversation_id (conversation_id),
    index idx_chat_message_create_time (create_time)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_unicode_ci comment='AI聊天消息表';

create table if not exists model_call_log (
    id varchar(64) not null comment '日志ID',
    conversation_id varchar(64) null comment '会话ID',
    message_id varchar(64) null comment '助手消息ID',
    model varchar(100) null comment '逻辑模型名称',
    provider varchar(50) null comment '模型厂商标识',
    request_body longtext null comment '序列化后的模型请求体',
    response_body longtext null comment '序列化后的模型响应体',
    status varchar(32) null comment '调用状态：SUCCESS或FAILED',
    error_message text null comment '调用失败时的错误信息',
    elapsed_ms bigint null comment '调用耗时，单位毫秒',
    create_time datetime null comment '创建时间',
    primary key (id),
    index idx_model_call_log_conversation_id (conversation_id),
    index idx_model_call_log_model (model),
    index idx_model_call_log_status (status),
    index idx_model_call_log_create_time (create_time)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_unicode_ci comment='模型调用日志表';
