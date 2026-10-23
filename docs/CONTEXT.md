# CONTEXT

## Módulos
- `br.coop.assembleia.pauta`: único módulo. API REST de pautas de assembleia.
  - `PautaController`: `POST /pautas` (201 com `{id}`), `GET /pautas/{id}` (200 ou 404).
  - `Pauta`: entidade JPA (`id`, `titulo` não nulo, `descricao` opcional).
  - `PautaRepository`: Spring Data JPA.
  - `CriarPautaRequest`: record com `@NotBlank` + `@Pattern` (exige caractere fora de `\p{Z}\p{C}\s`) + `@Size(max = 255)` em `titulo`; `@Size(max = 255)` em `descricao` (opcional).
  - `PautaResponse`, `PautaIdResponse`: DTOs de saída.

## Stack e comandos
- Java 21, Spring Boot 3.3.5 (parent), Maven, H2 em memória, JUnit 5, MockMvc, AssertJ.
- Build e testes: `mvnd test` (sem `clean`). Teste único: `mvnd test -Dtest='PautaControllerTest'`.
- Sem configuração externa: H2 é o banco padrão do teste e da aplicação.
- Dependências vêm do proxy interno (`ORQALIZ_ARTIFACT_PROXY_MAVEN_URL`). Sem internet, sem Docker.

## Convenções
- Pacote base `br.coop.assembleia.pauta`.
- Requests e responses como `record`. Entidade com construtor protegido para JPA.
- Erros 404 via `ResponseStatusException`. Erro 400 via Bean Validation (padrão Spring).
- Testes de integração MockMvc com `@SpringBootTest` + `@AutoConfigureMockMvc`, base limpa em `@BeforeEach`.

## Decisões
- Escopo fechado: só `POST /pautas` e `GET /pautas/{id}`. Sem votação, quórum, assembleia ou usuários.
- Persistência H2 em memória, sem Flyway/Liquibase (conforme task.md).
- Título validado com `@NotBlank`: rejeita vazio e só espaços.
- Descrição opcional, sem validação de tamanho (PO ainda não definiu).
- Formato de erro 400/404: padrão Spring (PO ainda não definiu).
- `java.time.Clock` não introduzido: nenhum campo de tempo nesta entrega.

- Autenticação/autorização: não implementada. Risco em aberto, não aceito pelo PO. `POST /pautas` e `GET /pautas/{id}` são públicos, ID é `IDENTITY` sequencial (enumeração/IDOR) e não há rate limit: crescimento ilimitado de memória (DoS) em H2 em memória. Pendência PO.
- Título com 255 caracteres aceito, 256 retorna 400 (evita 500 por estouro de `VARCHAR(255)`). Limite provisório até PO definir.
- Título sem caractere visível (vazio, espaços, NBSP U+00A0, largura zero U+200B) retorna 400.
- `descricao` sem trim e sem normalização: `""` é gravado como string vazia. Decisão pendente.

## Entregas
- Estrutura inicial do projeto Maven, `pom.xml`, aplicação, endpoints, testes MockMvc (AC-1.1 a AC-2.2, AC-3.1, AC-4.1).
- Correção de review: `@Size(max = 255)`, validação de whitespace invisível, teste 404 com id removido, JSON de teste via `ObjectMapper`.

## Armadilhas
- `PautaResponse.from` é package-private: se mover DTO para outro pacote, ajustar.
- Testes compartilham contexto H2; `@BeforeEach` limpa a base. Não remover essa limpeza.
- `mvnd dependency:resolve` pode imprimir lixo de log do jline; checar exit code, não só a saída.

## Pendências
- PO: limite de tamanho do título e da descrição (Could).
- PO: formato de erro 400/404 (padrão Spring ou corpo customizado).
- PO: autenticação/autorização de `POST /pautas` e `GET /pautas/{id}` (implementar ou aceitar risco formalmente).
- PO: limite definitivo de título e descrição (hoje 255 provisório).
- PO: normalização de trim e de descrição vazia.
- Sem varredura de dependências (SCA) no pipeline.
