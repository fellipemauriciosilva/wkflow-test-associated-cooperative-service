CREATE TABLE votes (
    id UUID PRIMARY KEY,
    agenda_id UUID NOT NULL,
    associate_id UUID NOT NULL,
    option VARCHAR(20) NOT NULL,
    event_id VARCHAR(255) UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_votes_agenda_id FOREIGN KEY (agenda_id) REFERENCES agendas(id) ON DELETE CASCADE,
    CONSTRAINT fk_votes_associate_id FOREIGN KEY (associate_id) REFERENCES associates(id) ON DELETE RESTRICT,
    CONSTRAINT uk_votes_agenda_associate UNIQUE (agenda_id, associate_id),
    CONSTRAINT uk_votes_event_id UNIQUE (event_id)
);

CREATE INDEX idx_votes_agenda_id ON votes(agenda_id);
CREATE INDEX idx_votes_associate_id ON votes(associate_id);
CREATE INDEX idx_votes_event_id ON votes(event_id);
