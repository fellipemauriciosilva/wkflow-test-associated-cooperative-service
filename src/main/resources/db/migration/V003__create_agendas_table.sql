CREATE TABLE agendas (
    id UUID PRIMARY KEY,
    assembly_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'CREATED',
    duration_in_minutes INTEGER NOT NULL DEFAULT 1,
    opened_at TIMESTAMP,
    closed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_agendas_assembly_id FOREIGN KEY (assembly_id) REFERENCES assemblies(id) ON DELETE CASCADE
);

CREATE INDEX idx_agendas_assembly_id ON agendas(assembly_id);
CREATE INDEX idx_agendas_status ON agendas(status);
CREATE INDEX idx_agendas_opened_at ON agendas(opened_at);
