-- ThinkChain RAG phase 2 MySQL metadata tables

create table if not exists knowledge_base (
    id varchar(64) not null comment '知识库ID',
    user_id varchar(64) null comment '所属用户ID',
    name varchar(100) not null comment '知识库名称',
    description varchar(500) null comment '知识库描述',
    status varchar(32) not null default 'ACTIVE' comment '知识库状态：ACTIVE启用、DISABLED停用',
    document_count int not null default 0 comment '文档数量',
    chunk_count int not null default 0 comment '切片数量',
    create_time datetime null comment '创建时间',
    update_time datetime null comment '更新时间',
    primary key (id),
    index idx_knowledge_base_user_id (user_id),
    index idx_knowledge_base_status (status)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_unicode_ci comment='知识库表';

create table if not exists knowledge_document (
    id varchar(64) not null comment '文档ID',
    knowledge_base_id varchar(64) not null comment '知识库ID',
    user_id varchar(64) null comment '上传用户ID',
    file_name varchar(255) not null comment '原始文件名',
    file_type varchar(50) null comment '文件类型',
    mime_type varchar(100) null comment '文件MIME类型',
    file_path varchar(500) null comment '文件存储路径',
    file_size bigint null comment '文件大小，单位字节',
    title varchar(255) null comment '文档标题',
    status varchar(32) not null default 'UPLOADED' comment '处理状态：UPLOADED已上传、PARSING解析中、CHUNKING切片中、EMBEDDING向量化中、COMPLETED完成、FAILED失败',
    chunk_count int not null default 0 comment '文档切片数量',
    error_message text null comment '处理失败时的错误信息',
    create_time datetime null comment '创建时间',
    update_time datetime null comment '更新时间',
    primary key (id),
    index idx_knowledge_document_kb_id (knowledge_base_id),
    index idx_knowledge_document_user_id (user_id),
    index idx_knowledge_document_status (status)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_unicode_ci comment='知识库文档表';

create table if not exists knowledge_chunk (
    id varchar(64) not null comment '切片ID',
    knowledge_base_id varchar(64) not null comment '知识库ID',
    document_id varchar(64) not null comment '文档ID',
    chunk_index int not null comment '切片序号',
    content text not null comment '切片文本内容',
    content_hash varchar(128) null comment '切片内容哈希',
    token_count int null comment '切片Token数量',
    char_count int null comment '切片字符数量',
    page_number int null comment '来源页码',
    section_title varchar(255) null comment '来源章节标题',
    embedding_status varchar(32) not null default 'PENDING' comment '向量化状态：PENDING待处理、COMPLETED完成、FAILED失败',
    vector_id varchar(64) null comment 'PostgreSQL向量记录ID',
    create_time datetime null comment '创建时间',
    update_time datetime null comment '更新时间',
    primary key (id),
    unique key uk_knowledge_chunk_doc_index (document_id, chunk_index),
    index idx_knowledge_chunk_kb_id (knowledge_base_id),
    index idx_knowledge_chunk_document_id (document_id),
    index idx_knowledge_chunk_vector_id (vector_id),
    index idx_knowledge_chunk_embedding_status (embedding_status)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_unicode_ci comment='知识库文档切片表';

create table if not exists rag_query_log (
    id varchar(64) not null comment 'RAG查询日志ID',
    conversation_id varchar(64) null comment '会话ID',
    knowledge_base_id varchar(64) not null comment '知识库ID',
    user_id varchar(64) null comment '查询用户ID',
    model varchar(100) null comment '回答使用的逻辑模型名称',
    embedding_model varchar(100) null comment '检索使用的向量模型名称',
    question text not null comment '用户问题',
    rewritten_query text null comment '改写后的检索问题',
    answer longtext null comment '模型回答内容',
    top_k int null comment '检索TopK数量',
    min_score decimal(10,6) null comment '最低相似度分数',
    hit_count int null comment '命中切片数量',
    hit_chunks longtext null comment '命中切片摘要JSON',
    elapsed_ms bigint null comment '总耗时，单位毫秒',
    status varchar(32) null comment '查询状态：SUCCESS成功、FAILED失败',
    error_message text null comment '失败时的错误信息',
    create_time datetime null comment '创建时间',
    primary key (id),
    index idx_rag_query_log_conversation_id (conversation_id),
    index idx_rag_query_log_kb_id (knowledge_base_id),
    index idx_rag_query_log_user_id (user_id),
    index idx_rag_query_log_status (status),
    index idx_rag_query_log_create_time (create_time)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_unicode_ci comment='RAG查询日志表';
