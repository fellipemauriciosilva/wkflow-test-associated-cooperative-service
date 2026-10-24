# Task: Fatia 1/3 - Pautas: cadastro e consulta

Épico: `run_94bb0d49e2da` (H91-VAL-EPICO)
Projeto: `wkflow-test-associated-cooperative-service`
Risco: baixo. Greenfield, sem legado, sem migração.

## Objetivo

Criar o módulo base do serviço (Spring Boot 3, Java 21, Maven, H2 em memória) e implementar o cadastro e a consulta de pautas, com validação de campos obrigatórios e tratamento padrão de erros. Esta fatia é a base das fatias 2 (sessão) e 3 (votos e resultado).

## Escopo

Dentro: estrutura Maven; H2 em memória; `POST /pautas`; `GET /pautas/{id}`; validação de `titulo` obrigatório (400); handler global de erros com formato único para 400 e 404; testes JUnit 5 e MockMvc.

Fora: sessão, votos, resultado (fatias 2 e 3); autenticação, persistência durável, notificação.

## Requisitos (MoSCoW)

- M1. Módulo Maven base sobe com Spring Boot 3 / Java 21 e H2 em memória.
- M2. `POST /pautas` cria pauta com `titulo` (obrigatório) e `descricao` (opcional). Retorna 201 com `id`.
- M3. `GET /pautas/{id}` retorna 200 com a pauta. Id inexistente: 404.
- M4. `POST /pautas` sem `titulo` (ausente ou vazio): 400, com mensagem indicando o campo.
- M5. Testes JUnit 5 + MockMvc cobrem M2, M3 e M4.
- S1. Erros 400 e 404 usam o mesmo formato de corpo, reutilizado nas fatias 2 e 3 (409).
- C1. Teste de contexto com H2 para validar o schema na subida.
- W1. Qualquer endpoint de sessão ou voto (fora).
- W2. Paginação ou listagem de pautas (fora).

## Requisitos de API

POST /pautas: body `{"titulo": "string (obrigatório)", "descricao": "string (opcional)"}`. 201 com `{"id", "titulo", "descricao"}`. 400 se `titulo` ausente, nulo ou só espaços.

GET /pautas/{id}: 200 com `{"id", "titulo", "descricao"}`. 404 se id inexistente.

Formato de erro (proposta, a confirmar): `{"status", "erro", "mensagem", "path"}`. Para 400, `erro` é `VALIDACAO_INVALIDA`.

## Critérios de aceite

- AC-1.1: POST /pautas com titulo válido cria pauta e retorna id (201).
- AC-1.2: GET /pautas/{id} retorna pauta existente com os mesmos campos cadastrados (200).
- AC-1.3: GET /pautas/{id} com id inexistente retorna 404 com erro NAO_ENCONTRADO.
- AC-1.4: POST /pautas sem titulo (ausente, nulo ou vazio) retorna 400 com erro VALIDACAO_INVALIDA e mensagem citando titulo.
- AC-1.5: Módulo sobe com Spring Boot 3, Java 21 e H2 em memória, com schema criado na subida.
- AC-1.6: Erros 400 e 404 seguem o mesmo formato de corpo (status, erro, mensagem, path).

## Restrições técnicas

- Java 21, Spring Boot 3, Spring MVC, Maven.
- H2 em memória. Schema na subida. Sem persistência entre reinícios.
- Id: chave gerada pelo banco, tipo `Long`.
- Bean Validation (`@NotBlank` em `titulo`). Tratamento centralizado em `@RestControllerAdvice`.
- Camadas: controller, service, repository (Spring Data JPA sobre H2).

## Premissas e lacunas

- Premissa: `titulo` vazio ou só espaços conta como ausente (400).
- Premissa: tamanho máximo de `titulo` e `descricao` não definido nesta fatia. Confirmar.
- Premissa: formato de erro vale para todo o épico. Confirmar.
- Lacuna herdada: prioridade e prazo do épico em aberto.
