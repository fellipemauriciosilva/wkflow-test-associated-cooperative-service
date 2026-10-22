# Contexto do projeto

## Módulos

- `com.acme.votacao` — classe main (`VotacaoPautasApplication`).
- `com.acme.votacao.pauta` — módulo único desta fatia: `Pauta` (entidade JPA), `PautaRepository` (Spring Data JPA), `PautaRequest`/`PautaResponse`/`PautaIdResponse` (DTOs), `PautaService` (regra de criação/busca), `PautaController` (`POST /pautas`, `GET /pautas/{id}`), `PautaNaoEncontradaException`, `GlobalExceptionHandler` (mapeia validação/JSON malformado/path var inválido → 400, pauta não encontrada → 404).

## Stack e comandos

- Java 21, Spring Boot 3.3.4 (web, data-jpa, validation), H2 em memória, Maven.
- Build/teste: `mvnd test` (sem `clean`). Build isolado: `mvnd compile`.
- Rodar local: `mvnd spring-boot:run`. Com console H2: `mvnd spring-boot:run -Dspring-boot.run.profiles=dev`.
- H2: `jdbc:h2:mem:votacao`, `ddl-auto: update`, sem migração (Flyway não usado — fora de escopo para greenfield pequeno com H2 em memória).

## Convenções

- DTOs como `record` (Java 21), sem Lombok (não adicionado ao pom).
- Validação via Bean Validation (`@NotBlank` em `titulo`, `@Size(max=255)` em `titulo` e `descricao`) em `PautaRequest`; `Pauta.titulo`/`Pauta.descricao` declaram `@Column(length = 255)` explícito (não depender do default do Hibernate, que coincide mas não é garantido). `GlobalExceptionHandler` captura `MethodArgumentNotValidException` → 400, `HttpMessageNotReadableException` (JSON malformado) → 400, `MethodArgumentTypeMismatchException` (ex. `{id}` não numérico) → 400, `PautaNaoEncontradaException` → 404. Corpo de erro vazio (RF-12, erro padronizado, está fora desta fatia).
- Limite de 255 em `titulo`/`descricao` conta **code units UTF-16** (`String.length()`), não bytes nem code points — documentado em README.md. Só diverge com pares substitutos (emoji fora do plano básico); ver testes `criar_tituloComCaracteresMultiByteNoLimite_retorna201` e `criar_tituloComSurrogatePairAcimaDoLimite_retorna400`.
- Pacote único `pauta` (sem camadas `api/application/domain/infrastructure` da skill genérica — escopo pequeno não justificou a separação).
- H2 console desabilitado no profile padrão (`spring.h2.console.enabled: false`); habilitado só sob profile `dev` (documento YAML multi-profile com separador `---` em `application.yml`).

## Decisões

- Testcontainers e Flyway (sugeridos pela skill `backend-java-springboot`) **não usados**: ambiente sem Docker/rede externa, e task.md pede explicitamente H2 em memória + MockMvc. Testes de controller usam `@SpringBootTest` + `@AutoConfigureMockMvc` com o H2 real (sem mocks de banco).
- Tamanho máximo de `titulo` e `descricao` fixado em 255 code units (premissa do task.md, seção 5, estendida a `descricao` após review apontar que a ausência de limite causava 500 — overflow do `VARCHAR(255)` default do H2 não era validado antes de chegar ao banco). Gera 400 se excedido em qualquer um dos dois. Ainda não confirmado com PO — revisar se divergir.
- `PautaNaoEncontradaException` mantida como classe própria (não fundida no handler), compatível com a lista de 10 arquivos prevista no épico.
- `docs/task.md` versionado nesta rodada: a spec da fatia 1 recebida no pedido de correção não existia no repo; sem ela, a verificação de AC-1.1..1.4 contra spec não era possível (apontado pelo review anterior).

## Entregas

- Fatia 1: `POST /pautas` (201/400), `GET /pautas/{id}` (200/404/400). AC-1.1 a AC-1.4 cobertos por `PautaControllerTest` (MockMvc, 14 testes: payload válido, busca por id existente/inexistente, titulo ausente/nulo/em branco, titulo e descricao em 255/256 chars, boundary multi-byte com acento e com emoji/par substituto, JSON malformado, id não numérico). `PautaService` coberto por `PautaServiceTest` (JUnit 5 + Mockito, 3 testes; `criar_persisteEPautaRetornada` agora assere `ArgumentCaptor` para titulo e descricao e `isSameAs` no valor retornado por `save`, não mais no stub). Suite completa: 17 testes, `mvnd test` → BUILD SUCCESS.
- Correções deste ciclo de review: limite de 255 em `descricao` (antes 500 em overflow), boundary tests 255/256/multi-byte, H2 console gated a profile `dev`, `docs/task.md` versionado, asserções de serviço reescritas para não depender do stub.

## Armadilhas

- `mvnd`/`mvn` neste ambiente resolvem dependências via proxy interno (`orqaliz-artifact-proxy`); primeira execução de `mvnd test` é lenta (download de plugins/deps). Sem internet real — se um artefato não existir no proxy, não há como obtê-lo de outra forma.
- Warnings de `jlinenative`/`jansi` ao rodar `mvnd` no console são ruído do daemon (terminal "dumb"), não indicam falha de build.
- `@Size(max=255)` sem `@Column(length=255)` explícito funciona por coincidência (default do Hibernate também é 255); se um dos dois limites mudar sozinho, o campo que passa a validação mas excede a coluna gera 500 em vez de 400. Sempre mudar os dois juntos.

## Pendências

- Fatias 2 (sessão de votação) e 3 (votos/resultado) do épico `run_2057fd08c4e4`, fora do escopo desta entrega.
- RF-12 (corpo padronizado de erro) e RF-13 (listagem paginada) marcados Won't/Could nesta fatia — não implementados.
- Confirmar com PO o limite de 255 code units (UTF-16) em `titulo`/`descricao` (ver Decisões e Convenções).
- Itens não bloqueantes do review de segurança, fora desta fatia, revisitar antes de qualquer deploy com exposição real: limite de tamanho de corpo de requisição, auth/rate-limit em `POST /pautas`, trim de espaços em `titulo`/`descricao`, ids sequenciais enumeráveis (IDOR se pautas ganharem regra de visibilidade).
