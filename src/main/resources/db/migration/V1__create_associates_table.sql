CREATE TABLE associates (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    document VARCHAR(50) NOT NULL,
    email VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_associates_document UNIQUE (document)
);
