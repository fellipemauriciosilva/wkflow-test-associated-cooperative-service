# CONTEXT.md

## Módulos

Projeto único Maven, pacote base `com.example.pauta`:

- `entity` — `Pauta` (JPA, Lombok `@Data`): `id` (Long, IDENTITY), `title` (`@NotBlank`), `createdAt` (`Instant`).
- `repository` — `PautaRepository extends JpaRepository<Pauta, Long>`.
- `service` — `PautaService`: `create(String title)` e `findById(Long id)` (lança `PautaNotFoundException` se ausente). Métodos `@Transactional`.
- `controller` — `PautaController`: `POST /pautas`, `GET /pautas/{id}`.
- `dto` — `PautaRequest` (record, `@NotBlank` no título), `PautaResponse` (record com `fromEntity`), `ErrorResponse` (record `error/timestamp/status`).
- `exception` — `PautaNotFoundException` (RuntimeException), `GlobalExceptionHandler` (`@RestControllerAdvice`): valida erros de `@Valid` → 400 com `ErrorResponse`; `PautaNotFoundException` → 404 sem corpo.

## Stack e comandos

- Java 21, Spring Boot 3.3.4 (`spring-boot-starter-web`, `-data-jpa`, `-validation`), H2 em memória, Lombok, JaCoCo 0.8.12.
- Build/test: `mvnd test` (sem `clean`; proxy Maven já configurado globalmente em `${MAVEN_HOME}/conf/settings.xml`, não mexer).
- Rodar 1 teste: `mvnd test -Dtest='PautaControllerTest#getNonExistentPauta_returns404'`.
- Relatório de cobertura: gerado automaticamente na fase `test` (plugin JaCoCo com execuções `prepare-agent` + `report`); ver `target/site/jacoco/index.html` ou `target/site/jacoco/jacoco.csv`.
- `application.properties` (main): `jdbc:h2:mem:pautadb`, `ddl-auto=create-drop`. `application.properties` (test): `jdbc:h2:mem:pautadb-test`, mesmo `ddl-auto`.

## Convenções

- DTOs como `record` (não Lombok) — mais simples para payloads imutáveis; só a entidade `Pauta` usa Lombok `@Data`/`@NoArgsConstructor`/`@AllArgsConstructor` porque JPA exige construtor vazio e setters.
- Mensagem de validação do título é fixa: `"title must not be blank"` (duplicada em `Pauta.title` e `PautaRequest.title` — ambas devem ser mantidas iguais se o texto mudar, já que o teste de validação faz `containsString("title")`).
- 404 de `GET /pautas/{id}` retorna corpo vazio (sem JSON), conforme AC-1.4 ("no JSON body or error message").
- Erros de validação (400) usam `ErrorResponse` com `error`/`timestamp`/`status`.

## Decisões

- `GlobalExceptionHandler` centraliza tratamento de erro em vez de try/catch no controller — único handler cobre todos os endpoints futuros do módulo pauta.
- `spring.jpa.open-in-view=false` em ambos os `application.properties` para evitar conexão de banco aberta durante serialização da resposta (boa prática padrão, não é requisito do spec).
- Testes usam `@SpringBootTest` + `@AutoConfigureMockMvc` + `@Transactional` (rollback automático por teste) em vez de `@WebMvcTest`, porque AC-1.5 pede verificação direta via `PautaRepository` na mesma transação.

## Entregas

- Slice 1 completo: entidade, repositório, serviço, controller, DTOs, exceptions, validação, 7 testes JUnit 5 + MockMvc cobrindo AC-1.1 a AC-1.7 (criação válida, rejeição de título nulo/vazio/blank/ausente, GET existente, GET 404, persistência direta via repositório, timestamp auto-populado, métodos HTTP não suportados → 405).
- `mvnd test`: 7/7 testes passando, `BUILD SUCCESS`.
- Cobertura JaCoCo: 97% de instruções, 94% de linhas no total do projeto (bem acima dos 80% exigidos); único ponto não coberto é `PautaApplication.main` (bootstrap do Spring, não testável via MockMvc).

## Armadilhas

- Saída de `mvnd`/Java no terminal deste ambiente é truncada/reformatada por uma ferramenta de busca de texto interna do harness (aparece como "N matches in F" em vez do log puro) — sempre redirecionar para arquivo (`mvnd test > /tmp/log 2>&1`) e usar `grep`/`cat` no arquivo para ler o resultado real.
- `mvnd` emite avisos de `jline`/`jansi` no stderr (bibliotecas nativas não carregam no container) — são apenas ruído, não indicam falha de build.
- Não criar `~/.m2/settings.xml`: o proxy Maven já está fixado em `${MAVEN_HOME}/conf/settings.xml` com `mirrorOf=*`; um settings de usuário duplicado pode conflitar.

## Pendências

- Nenhuma pendência conhecida para o Slice 1. Próximos slices do épico H91-06B (ex.: votação, associados) ainda não iniciados neste repositório.
