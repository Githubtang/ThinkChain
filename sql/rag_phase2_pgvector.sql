-- ThinkChain RAG phase 2 PostgreSQL pgvector tables
-- 本脚本允许重复执行：不会删除已有向量数据。

create extension if not exists vector;

create table if not exists rag_embedding (
    id varchar(64) not null,
    scope_type varchar(20) not null,
    scope_id varchar(64) not null,
    knowledge_base_id varchar(64) null,
    conversation_id varchar(64) null,
    document_id varchar(64) not null,
    chunk_id varchar(64) not null,
    embedding_model varchar(100) not null,
    content text not null,
    embedding vector (1536) not null,
    metadata jsonb null,
    create_time timestamp without time zone not null default current_timestamp,
    primary key (id)
);

comment on table rag_embedding is 'RAG向量存储表';

comment on column rag_embedding.id is '向量记录ID';

comment on column rag_embedding.scope_type is 'RAG作用域类型：KB知识库、SESSION会话临时文件';

comment on column rag_embedding.scope_id is 'RAG作用域ID：知识库ID或会话ID';

comment on column rag_embedding.knowledge_base_id is '知识库ID';

comment on column rag_embedding.conversation_id is '会话ID，临时文件向量使用';

comment on column rag_embedding.document_id is '文档ID';

comment on column rag_embedding.chunk_id is '切片ID';

comment on column rag_embedding.embedding_model is '向量模型名称';

comment on column rag_embedding.content is '切片文本内容';

comment on column rag_embedding.embedding is '文本向量，维度需要与向量模型保持一致';

comment on column rag_embedding.metadata is '向量元数据JSON';

comment on column rag_embedding.create_time is '创建时间';

create index if not exists idx_rag_embedding_scope on rag_embedding (scope_type, scope_id);

create index if not exists idx_rag_embedding_kb_id on rag_embedding (knowledge_base_id);

create index if not exists idx_rag_embedding_conversation_id on rag_embedding (conversation_id);

create index if not exists idx_rag_embedding_document_id on rag_embedding (document_id);

-- 每个切片只允许有一条向量。若旧版本产生过重复数据，为每个 chunk_id 保留一条。
delete from rag_embedding older
using rag_embedding newer
where older.chunk_id = newer.chunk_id
  and older.ctid < newer.ctid;

drop index if exists idx_rag_embedding_chunk_id;
create unique index if not exists uk_rag_embedding_chunk_id on rag_embedding (chunk_id);

create index if not exists idx_rag_embedding_model on rag_embedding (embedding_model);

create index if not exists idx_rag_embedding_create_time on rag_embedding (create_time);

-- HNSW 可以在空表创建，后续写入数据时索引会自动维护；查询使用余弦距离 <=>。
drop index if exists idx_rag_embedding_vector_cosine;
create index if not exists idx_rag_embedding_vector_cosine_hnsw
    on rag_embedding using hnsw (embedding vector_cosine_ops);

-- rag_embedding 只供后端 JDBC 使用，不通过 Supabase Data API 暴露给前端角色。
alter table rag_embedding enable row level security;
revoke all on table rag_embedding from anon, authenticated;
