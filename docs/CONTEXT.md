# Contexto do projeto

## Módulos

- `votacao.pauta`: entidade `Pauta`, repository, service, controller, DTOs `PautaRequest` e `PautaResponse`, `PautaNaoEncontradaException`.
- `votacao.web`: `ApiExceptionHandler` (formato único de erro `ErroResponse`) e `LimiteBodyFilter` (limite de tamanho do body).

## Stack e comandos

- Java 21, Spring Boot 3.3.5, Maven, Spring Web MVC, Spring Data JPA (Hibernate 6.5), Bean Validation.
- H2 em memória, `ddl-auto: create-drop` (`application.yml`).
- Testes: JUnit 5, MockMvc (`@SpringBootTest` + `@AutoConfigureMockMvc`), `@DataJpaTest`.
- Comandos: `mvnd test` (suite inteira), `mvnd test -Dtest='PautaControllerTest'` (classe).

## Convenções

- Erros sempre no formato `ErroResponse`: `timestamp`, `status`, `error`, `message`, `path`.
- Mensagens de erro de framework (Jackson, conversão de tipo) são fixas no handler; `ex.getMessage()` não é repassado.
- Validação de campos: `@NotBlank`/`@Size` no DTO de entrada, `@Valid` no controller.

## Decisões

- `titulo`: obrigatório, em branco não é aceito, máximo 255 caracteres (`PautaRequest.TITULO_MAX`).
- `descricao`: opcional, ausente ou `null` é aceita e devolvida como `null`, máximo 1000 caracteres (`PautaRequest.DESCRICAO_MAX`).
- Acima do limite de campo: 400 com `message` listando o campo (`titulo: ...`, `descricao: ...`).
- Limite do body: 16 KB (`LimiteBodyFilter.TAMANHO_MAXIMO_BYTES`). Com `Content-Length` acima do limite, o filtro responde 413 com `ErroResponse` direto. Sem o header (chunked), `getInputStream()` e `getReader()` contam bytes lidos; ao estourar, `LimiteExcedidoException` vira 413 pelo `ApiExceptionHandler` (`message` `LimiteBodyFilter.MENSAGEM_LIMITE`). Coberto por `LimiteBodyChunkedTest` (RANDOM_PORT, chunked real) e `LimiteBodyFilterTest`.
- JSON malformado: 400, `message` fixa `Corpo da requisição inválido`.
- `id` não numérico em `GET /pautas/{id}`: 400, `message` fixa `Parâmetro 'id' inválido`.
- Pauta inexistente: 404, `message` `Pauta não encontrada: {id}`.
- `POST /pautas` retorna 201 com header `Location: /pautas/{id}` e body com o recurso criado.

## Entregas

- Fatia 1 (cadastro e consulta de pauta): `POST /pautas`, `GET /pautas/{id}`, tratamento de erros padronizado, limites de tamanho, testes MockMvc e de persistência.
- Ciclo de correção do review: handlers para JSON malformado e `id` inválido, `@Size` com limites, filtro de body, asserções exatas de `Location` e mensagem 404.

## Armadilhas

- Limite de body é aplicado na leitura: o Jackson só consome até 16 KB, o restante não entra em memória. Proxy/gateway em produção continua necessário para limitar conexões.
- Erros fora de `ApiExceptionHandler` (ex.: 500 inesperado, 405, 415, 404 de rota) seguem o formato padrão do Spring Boot, não `ErroResponse`.
- `create-drop` apaga os dados a cada restart. Não usar em produção.

## Pendências

- Autenticação e autorização: POST e GET estão abertos, com IDs sequenciais. Fora do escopo da fatia 1 (W3 do ticket). Decidir antes de subir.
- Limite de body para requisições chunked: a aplicação já corta na leitura. Mesmo assim, configurar `client_max_body_size` (ou equivalente) no proxy/gateway como defesa em profundidade. Fora do escopo do código.
- GATE DE DEPLOY: `GET /pautas/{id}` e `POST /pautas` sem autenticação, com IDs sequenciais (enumeração de todas as pautas). Decidir autenticação antes de subir.
- Erros 405, 415 e 404 de rota ainda no formato padrão do Spring Boot. Tratar em `ApiExceptionHandler` ou documentar exceção.
- `parametroInvalido` fixa `'id'`. Trocar por `ex.getName()` antes das fatias 2/3 com outros parâmetros.
- Asserts de mensagem de validação em `PautaControllerTest` usam `containsString`. Apertar para mensagem exata.
- Escape de `descricao` no front-end (XSS armazenado): definir quem escapa. A API grava texto puro.
- Regra de unicidade de título: pergunta de negócio, não implementada.
- Profile de produção: `ddl-auto: validate` e banco real, separado do `application.yml`.
- Dependências: verificar CVEs do Spring Boot 3.3.5 e adicionar análise (ex.: OWASP dependency-check).
- Listagem, edição e exclusão de pauta: fatias 2 e 3 (W1, W2).
