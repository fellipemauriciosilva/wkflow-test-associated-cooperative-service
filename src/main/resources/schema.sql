CREATE TABLE IF NOT EXISTS pauta (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    titulo VARCHAR(255) NOT NULL,
    descricao VARCHAR(2000)
);

CREATE TABLE IF NOT EXISTS sessao (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    pauta_id BIGINT NOT NULL,
    abertura TIMESTAMP WITH TIME ZONE NOT NULL,
    fim TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_sessao_pauta FOREIGN KEY (pauta_id) REFERENCES pauta (id)
);

CREATE TABLE IF NOT EXISTS voto (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    pauta_id BIGINT NOT NULL,
    associado_id VARCHAR(255) NOT NULL,
    opcao VARCHAR(3) NOT NULL,
    CONSTRAINT fk_voto_pauta FOREIGN KEY (pauta_id) REFERENCES pauta (id),
    CONSTRAINT uk_voto_pauta_associado UNIQUE (pauta_id, associado_id)
);
