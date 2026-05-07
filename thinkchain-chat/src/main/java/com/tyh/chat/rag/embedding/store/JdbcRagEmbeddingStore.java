package com.tyh.chat.rag.embedding.store;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Repository
@ConditionalOnBean(name = "vectorJdbcTemplate")
public class JdbcRagEmbeddingStore implements RagEmbeddingStore {

    private final JdbcTemplate jdbcTemplate;

    public JdbcRagEmbeddingStore(@Qualifier("vectorJdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public int save(RagEmbeddingRecord record) {
        String scopeType = firstNonBlank(record.getScopeType(), isNonBlank(record.getConversationId()) ? "SESSION" : "KB");
        String scopeId = firstNonBlank(record.getScopeId(), "SESSION".equalsIgnoreCase(scopeType)
                ? record.getConversationId()
                : record.getKnowledgeBaseId());
        String sql = """
                insert into rag_embedding (
                    id, scope_type, scope_id, knowledge_base_id, conversation_id, document_id,
                    chunk_id, embedding_model, content, embedding, metadata
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, cast(? as vector), cast(? as jsonb))
                on conflict (id) do update set
                    scope_type = excluded.scope_type,
                    scope_id = excluded.scope_id,
                    knowledge_base_id = excluded.knowledge_base_id,
                    conversation_id = excluded.conversation_id,
                    document_id = excluded.document_id,
                    chunk_id = excluded.chunk_id,
                    embedding_model = excluded.embedding_model,
                    content = excluded.content,
                    embedding = excluded.embedding,
                    metadata = excluded.metadata
                """;
        return jdbcTemplate.update(sql,
                record.getId(),
                scopeType,
                scopeId,
                record.getKnowledgeBaseId(),
                record.getConversationId(),
                record.getDocumentId(),
                record.getChunkId(),
                record.getEmbeddingModel(),
                record.getContent(),
                toVectorLiteral(record.getEmbedding()),
                record.getMetadata() != null ? record.getMetadata() : "{}");
    }

    @Override
    public int deleteByChunkId(String chunkId) {
        return jdbcTemplate.update("delete from rag_embedding where chunk_id = ?", chunkId);
    }

    @Override
    public int deleteByDocumentId(String documentId) {
        return jdbcTemplate.update("delete from rag_embedding where document_id = ?", documentId);
    }

    @Override
    public List<RagEmbeddingMatch> search(String knowledgeBaseId, float[] queryEmbedding, int topK) {
        return searchByScope("KB", knowledgeBaseId, queryEmbedding, topK);
    }

    @Override
    public List<RagEmbeddingMatch> searchByScope(String scopeType, String scopeId, float[] queryEmbedding, int topK) {
        return searchByScope(scopeType, scopeId, Collections.emptyList(), queryEmbedding, topK);
    }

    @Override
    public List<RagEmbeddingMatch> searchByScope(String scopeType, String scopeId, List<String> documentIds,
                                                 float[] queryEmbedding, int topK) {
        StringBuilder sql = new StringBuilder("""
                select id, scope_type, scope_id, knowledge_base_id, conversation_id, document_id, chunk_id, content,
                       1 - (embedding <=> cast(? as vector)) as score
                from rag_embedding
                where scope_type = ?
                  and scope_id = ?
                """);
        List<Object> args = new ArrayList<>();
        String vector = toVectorLiteral(queryEmbedding);
        args.add(vector);
        args.add(scopeType);
        args.add(scopeId);
        if (documentIds != null && !documentIds.isEmpty()) {
            sql.append(" and document_id in (");
            for (int i = 0; i < documentIds.size(); i++) {
                if (i > 0) {
                    sql.append(", ");
                }
                sql.append("?");
                args.add(documentIds.get(i));
            }
            sql.append(")\n");
        }
        sql.append("""
                order by embedding <=> cast(? as vector)
                limit ?
                """);
        args.add(vector);
        args.add(topK);
        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> {
            RagEmbeddingMatch match = new RagEmbeddingMatch();
            match.setId(rs.getString("id"));
            match.setScopeType(rs.getString("scope_type"));
            match.setScopeId(rs.getString("scope_id"));
            match.setKnowledgeBaseId(rs.getString("knowledge_base_id"));
            match.setConversationId(rs.getString("conversation_id"));
            match.setDocumentId(rs.getString("document_id"));
            match.setChunkId(rs.getString("chunk_id"));
            match.setContent(rs.getString("content"));
            match.setScore(rs.getDouble("score"));
            return match;
        }, args.toArray());
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a;
        }
        return b;
    }

    private static boolean isNonBlank(String value) {
        return value != null && !value.isBlank();
    }

    private static String toVectorLiteral(float[] vector) {
        if (vector == null || vector.length == 0) {
            throw new IllegalArgumentException("Embedding vector must not be empty");
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(vector[i]);
        }
        sb.append(']');
        return sb.toString();
    }
}
