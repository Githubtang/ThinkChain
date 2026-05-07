-- ThinkChain RAG phase 2 PostgreSQL pgvector tables

create extension if not exists vector;

create table if not exists rag_embedding (
    id varchar(64) not null,
    knowledge_base_id varchar(64) not null,
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

comment on column rag_embedding.knowledge_base_id is '知识库ID';

comment on column rag_embedding.document_id is '文档ID';

comment on column rag_embedding.chunk_id is '切片ID';

comment on column rag_embedding.embedding_model is '向量模型名称';

comment on column rag_embedding.content is '切片文本内容';

comment on column rag_embedding.embedding is '文本向量，维度需要与向量模型保持一致';

comment on column rag_embedding.metadata is '向量元数据JSON';

comment on column rag_embedding.create_time is '创建时间';

create index if not exists idx_rag_embedding_kb_id on rag_embedding (knowledge_base_id);

create index if not exists idx_rag_embedding_document_id on rag_embedding (document_id);

create index if not exists idx_rag_embedding_chunk_id on rag_embedding (chunk_id);

create index if not exists idx_rag_embedding_model on rag_embedding (embedding_model);

create index if not exists idx_rag_embedding_create_time on rag_embedding (create_time);

create index if not exists idx_rag_embedding_vector_cosine on rag_embedding using ivfflat (embedding vector_cosine_ops)
with (lists = 100);