package com.tyh.chat.rag.embedding.store;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

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
        String sql = """
                insert into rag_embedding (
                    id, knowledge_base_id, document_id, chunk_id, embedding_model, content, embedding, metadata
                ) values (?, ?, ?, ?, ?, ?, cast(? as vector), cast(? as jsonb))
                on conflict (id) do update set
                    knowledge_base_id = excluded.knowledge_base_id,
                    document_id = excluded.document_id,
                    chunk_id = excluded.chunk_id,
                    embedding_model = excluded.embedding_model,
                    content = excluded.content,
                    embedding = excluded.embedding,
                    metadata = excluded.metadata
                """;
        return jdbcTemplate.update(sql,
                record.getId(),
                record.getKnowledgeBaseId(),
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
    public List<RagEmbeddingMatch> search(String knowledgeBaseId, float[] queryEmbedding, int topK) {
        String sql = """
                select id, knowledge_base_id, document_id, chunk_id, content,
                       1 - (embedding <=> cast(? as vector)) as score
                from rag_embedding
                where knowledge_base_id = ?
                order by embedding <=> cast(? as vector)
                limit ?
                """;
        String vector = toVectorLiteral(queryEmbedding);
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            RagEmbeddingMatch match = new RagEmbeddingMatch();
            match.setId(rs.getString("id"));
            match.setKnowledgeBaseId(rs.getString("knowledge_base_id"));
            match.setDocumentId(rs.getString("document_id"));
            match.setChunkId(rs.getString("chunk_id"));
            match.setContent(rs.getString("content"));
            match.setScore(rs.getDouble("score"));
            return match;
        }, vector, knowledgeBaseId, vector, topK);
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
