CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE memory_embeddings (
    id VARCHAR(64) PRIMARY KEY,
    owner_type VARCHAR(64) NOT NULL,
    owner_id VARCHAR(64) NOT NULL,
    embedding_type VARCHAR(64) NOT NULL,
    model_name VARCHAR(128) NOT NULL,
    dimensions INTEGER NOT NULL,
    content_summary TEXT NOT NULL,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    vector VECTOR(1024),
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX memory_embeddings_vector_hnsw_idx
    ON memory_embeddings USING hnsw (vector vector_cosine_ops);
