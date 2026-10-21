# Contexto do projeto — votacao-api

Épico: H90-15-EPICO / run_bfe1226524a9. Slice 1/3 entregue neste commit.

## Módulos

- `com.votacao` — `Application` (bean `Clock` system default).
- `com.votacao.domain.pauta` — entidade `Pauta`, `PautaRepository`, `PautaNotFoundException`.
- `com.votacao.domain.sessao` — entidade `Sessao`, `SessaoRepository`, `SessaoJaAbertaException`.
- `com.votacao.application.pauta` — `PautaService` (criar, buscarPorId).
- `com.votacao.application.sessao` — `SessaoService` (abrir, com `Clock` injetado).
- `com.votacao.api.pauta` — `PautaController` + DTOs (`PautaRequest`, `PautaResponse`).
- `com.votacao.api.sessao` — `SessaoController` + DTOs (`SessaoRequest`, `SessaoResponse`).
- `com.votacao.infrastructure.error` — `ApiErrorHandler` (`@RestControllerAdvice`), `ErrorResponse`.

## Stack e comandos

- Java 21, Spring Boot 3.3.4, Spring Web, Spring Data JPA, Bean Validation, H2 em memória.
- Build: Maven. Usar `mvnd` (daemon quente). `mvn` puro também funciona (mais lento).
- `mvnd test` roda a suite. `mvnd spring-boot:run` sobe a API em `/api`.
- **Armadilha de ambiente:** `/tmp` está montado `noexec` neste ambiente — jansi/jline
  (usados pelo mvnd) falham ao carregar libs nativas em `/tmp`. Workaround: passar
  `-Djansi.tmpdir=./.tmp -Djline.tmpdir=./.tmp -Djava.io.tmpdir=./.tmp` (diretório `.tmp/`
  dentro do projeto, ignorado no `.gitignore`). Não afeta o resultado do build, só a saída
  de log/warning.

## Convenções

- Pacotes por camada (`api` / `application` / `domain` / `infrastructure`), sub-pacote por
  domínio (`pauta`, `sessao`).
- Entidades JPA imutáveis do ponto de vista de API pública: sem setters, construtor com
  campos obrigatórios, construtor protegido sem args para o Hibernate.
- DTOs como `record`. Campo `duracao_minutos` em JSON mapeado via `@JsonProperty` em
  `SessaoRequest` (campo Java `duracaoMinutos`).
- Erros: todas as exceções de domínio capturadas em `ApiErrorHandler`, resposta sempre
  `{"error", "timestamp", "path"}`.
- Testes de service: JUnit 5 + Mockito (`@ExtendWith(MockitoExtension.class)`), `Clock.fixed`
  para determinismo em `SessaoServiceTest`.
- Testes de controller: `@SpringBootTest` + `@AutoConfigureMockMvc`, contexto completo com H2,
  paths relativos ao context-path (`/pautas/...`), já que MockMvc com contexto Spring Boot
  completo aplica `server.servlet.context-path` automaticamente.

## Decisões

- Context-path `/api` fixado em `application.properties`. Testes MockMvc usam paths sem o
  prefixo (`/pautas`) porque o `DispatcherServlet` de teste já aplica o context-path.
- Concorrência na abertura de sessão: dupla barreira — check `findByPautaId` no service (rejeita
  cedo) + constraint única de BD em `sessao.pauta_id` (`SessaoRepository`/`Sessao` com
  `@JoinColumn(unique = true)`), com `DataIntegrityViolationException` traduzida para
  `SessaoJaAbertaException` → 422. Não há teste de concorrência real (threads paralelas) nesta
  entrega; a cobertura de AC-2.6 é via teste unitário simulando a exceção de constraint.
- Duração de sessão: sem teto superior implementado (épico não define limite). Pendência aberta,
  listada abaixo.

## Entregas

- Slice 1/3 completo: CRUD básico de pauta (criar, consultar) + abertura de sessão de votação
  com duração parametrizável. 20 testes (unitários + MockMvc), suite verde.
- Endpoints: `POST /api/pautas`, `GET /api/pautas/{id}`, `POST /api/pautas/{id}/sessao`.

## Armadilhas

- `/tmp` noexec quebra jansi/jline do `mvnd` — ver seção Stack e comandos acima.
- `SessaoRequest` com corpo HTTP vazio (sem `Content-Type`/body): controller trata `request == null`
  criando `SessaoRequest(null)` manualmente, pois `@RequestBody(required = false)` com body
  ausente chega como `null` em vez de lançar erro.

## Pendências

- Limite superior para `duracao_minutos`: não implementado, não definido pelo épico. Confirmar
  com stakeholder antes do merge final (mencionado no `task.md` original como questão em aberto).
- Teste de concorrência real (múltiplas threads/requests paralelas batendo no mesmo endpoint)
  não foi implementado — AC-2.6 coberto apenas pela simulação unitária da constraint de BD.
- Pré-requisito para slice 2 (voto): `Pauta` e `Sessao` estáveis, `PautaRepository.findById`
  disponível — atendido.
