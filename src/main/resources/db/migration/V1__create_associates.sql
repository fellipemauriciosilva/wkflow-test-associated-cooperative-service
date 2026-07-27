CREATE TABLE associates (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    document VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_document ON associates(document);
CREATE INDEX idx_created_at ON associates(created_at);
