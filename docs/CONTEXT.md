# CONTEXT.md

## Módulos

Projeto único Maven, pacote base `com.example.pauta`:

- `entity` — `Pauta` (JPA, Lombok `@Data`): `id` (Long, IDENTITY), `title` (`@NotBlank` + `@Size(1,255)`), `createdAt` (`Instant`).
- `repository` — `PautaRepository extends JpaRepository<Pauta, Long>`.
- `service` — `PautaService`: `create(String title)` (faz `title.trim()` antes de salvar) e `findById(Long id)` (lança `PautaNotFoundException` se ausente). Métodos `@Transactional`.
- `controller` — `PautaController`: `POST /pautas`, `GET /pautas/{id}`.
- `dto` — `PautaRequest` (record, `@NotBlank` + `@Size(1,255)` no título), `PautaResponse` (record com `fromEntity`), `ErrorResponse` (record `error/timestamp/status`).
- `exception` — `PautaNotFoundException` (RuntimeException), `GlobalExceptionHandler` (`@RestControllerAdvice`): valida erros de `@Valid` → 400 com `ErrorResponse`; `HttpMessageNotReadableException` (JSON malformado) → 400 genérico com log interno (sem stack trace no corpo); `PautaNotFoundException` → 404 sem corpo.
- `config` — `JacksonConfig`: customiza o `ObjectMapper` global via `Jackson2ObjectMapperBuilderCustomizer` (`FAIL_ON_UNKNOWN_PROPERTIES=true`, `ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT=false`) para impedir injeção silenciosa de campos e bypass de tipo.

## Stack e comandos

- Java 21, Spring Boot 3.3.4 (`spring-boot-starter-web`, `-data-jpa`, `-validation`), H2 em memória, Lombok, JaCoCo 0.8.12.
- Build/test: `mvnd test` (sem `clean`; proxy Maven já configurado globalmente em `${MAVEN_HOME}/conf/settings.xml`, não mexer).
- Rodar 1 teste: `mvnd test -Dtest='PautaControllerTest#getNonExistentPauta_returns404'`.
- Relatório de cobertura: gerado automaticamente na fase `test` (plugin JaCoCo com execuções `prepare-agent` + `report`); ver `target/site/jacoco/index.html` ou `target/site/jacoco/jacoco.csv`.
- `application.properties` (main): `jdbc:h2:mem:pautadb`, `ddl-auto=create-drop`. `application.properties` (test): `jdbc:h2:mem:pautadb-test`, mesmo `ddl-auto`.

## Convenções

- DTOs como `record` (não Lombok) — mais simples para payloads imutáveis; só a entidade `Pauta` usa Lombok `@Data`/`@NoArgsConstructor`/`@AllArgsConstructor` porque JPA exige construtor vazio e setters.
- Mensagem de validação do título é fixa: `"title must not be blank"` (duplicada em `Pauta.title` e `PautaRequest.title` — ambas devem ser mantidas iguais se o texto mudar, já que o teste de validação faz `containsString("title")`). Limite de tamanho usa mensagem separada: `"title must be between 1 and 255 characters"`.
- 404 de `GET /pautas/{id}` retorna corpo vazio (sem JSON), conforme AC-1.4 ("no JSON body or error message").
- Erros de validação e de JSON malformado (400) usam `ErrorResponse` com `error`/`timestamp`/`status`.
- Testes de `PautaControllerTest` usam `@BeforeEach pautaRepository.deleteAll()` (isolamento explícito) em vez de `@Transactional` de classe — cada caso de rejeição de título (null/vazio/blank/ausente) é um teste próprio com sua própria asserção `pautaRepository.count()`.

## Decisões

- `GlobalExceptionHandler` centraliza tratamento de erro em vez de try/catch no controller — único handler cobre todos os endpoints futuros do módulo pauta.
- `spring.jpa.open-in-view=false` em ambos os `application.properties` para evitar conexão de banco aberta durante serialização da resposta (boa prática padrão, não é requisito do spec).
- Testes usam `@SpringBootTest` + `@AutoConfigureMockMvc`, com `@BeforeEach` limpando o repositório em vez de depender de rollback transacional de classe — torna a isolação de teste explícita e não mágica (feedback do review).
- `title` é trimado no service antes de salvar (`title.trim()`), e limitado a 255 caracteres via `@Size` na entidade e no DTO — mitiga DoS por string ilimitada (feedback de segurança do review).
- `HttpMessageNotReadableException` (JSON malformado/corrompido) tem handler dedicado que loga a exceção completa internamente mas retorna mensagem genérica ao cliente — evita vazar stack trace/versões de framework (feedback de segurança do review).
- `JacksonConfig` desabilita coerções silenciosas (`ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT`) e rejeita propriedades desconhecidas (`FAIL_ON_UNKNOWN_PROPERTIES`) no `ObjectMapper` global via `Jackson2ObjectMapperBuilderCustomizer` — evita bypass de validação (`{"title": []}` → null) e injeção de campos extras no payload.

## Entregas

- Slice 1 completo + ciclo de correção do review (CHANGES_REQUIRED): entidade, repositório, serviço, controller, DTOs, exceptions, validação, Jackson hardening, 11 testes JUnit 5 + MockMvc cobrindo AC-1.1 a AC-1.7.
- Correções aplicadas: `@Size(1,255)` + trim no título (DoS); handler de `HttpMessageNotReadableException` (vazamento de stack trace); verificação de persistência via `PautaRepository` nos testes de criação e de GET; split do teste de validação em 4 testes isolados (null/vazio/blank/ausente), cada um com `pautaRepository.count()`; `JacksonConfig` com `FAIL_ON_UNKNOWN_PROPERTIES`/`ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT`; troca de `@Transactional` de classe por `@BeforeEach deleteAll()`.
- `mvnd test`: 11/11 testes passando, `BUILD SUCCESS`.
- Cobertura JaCoCo: todas as classes de produção (`PautaService`, `GlobalExceptionHandler`, `PautaController`, DTOs, `JacksonConfig`) com 0 instruções não cobertas; único ponto não coberto é `PautaApplication.main` (bootstrap do Spring, não testável via MockMvc).

## Armadilhas

- Saída de `mvnd`/Java no terminal deste ambiente é truncada/reformatada por uma ferramenta de busca de texto interna do harness (aparece como "N matches in F" em vez do log puro) — sempre redirecionar para arquivo (`mvnd test > /tmp/log 2>&1`) e usar `grep`/`cat` no arquivo para ler o resultado real.
- `mvnd` emite avisos de `jline`/`jansi` no stderr (bibliotecas nativas não carregam no container) — são apenas ruído, não indicam falha de build.
- Não criar `~/.m2/settings.xml`: o proxy Maven já está fixado em `${MAVEN_HOME}/conf/settings.xml` com `mirrorOf=*`; um settings de usuário duplicado pode conflitar.
- `DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL` não existe nesta versão do Jackson (2.17.x); o nome correto é `ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT` — erro de compilação se usar o nome errado.
- `createdAt` pode perder precisão de nanossegundos ao ir e voltar do H2 (ex.: `...135Z` vira `...135134952Z` truncado); comparar por janela de tempo (`Instant.toEpochMilli()` com tolerância), nunca por igualdade exata de string entre duas chamadas (POST vs GET).

## Pendências

- Nenhuma pendência conhecida para o Slice 1 após o ciclo de correção. Os 2 pontos restantes do relatório de segurança (race condition/optimistic locking e timing attack no 404) foram avaliados como fora de escopo do MVP da Slice 1 — spec não exige concorrência nem mitigação de timing attack nesta fase; revisitar se Slice 2+ adicionar DELETE/PATCH ou exigir hardening adicional.
- Próximos slices do épico H91-06B (ex.: votação, associados) ainda não iniciados neste repositório.
