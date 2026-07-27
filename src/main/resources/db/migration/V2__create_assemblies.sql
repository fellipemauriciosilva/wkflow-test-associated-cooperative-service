CREATE TABLE assemblies (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    scheduled_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_title ON assemblies(title);
CREATE INDEX idx_created_at ON assemblies(created_at);
CREATE INDEX idx_scheduled_at ON assemblies(scheduled_at);
