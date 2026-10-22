# Contexto do projeto

## Spec

Épico H90-17 (votação de assembleia), 3 fatias. Fatia 1 (pautas) e fatia 2 (sessão)
entregues. AC → teste:

| AC | Descrição | Teste(s) |
|---|---|---|
| AC-1.1–1.6 | Cadastro/consulta de pauta | `PautaControllerTest`, `PautaTest` — ver histórico abaixo |
| AC-2.1 | POST sessão com duração informada → 2xx com id | `SessaoControllerTest.abrir_comDuracaoInformada_retorna2xxComId` |
| AC-2.2 | Duração omitida/`null` → default 1 minuto | `SessaoControllerTest.abrir_semCorpo_...`/`abrir_comDuracaoNula_...`, `SessaoServiceTest.abrir_comDuracaoNula_assumeUmMinuto` |
| AC-2.3 | Duração <= 0 → 400 | `SessaoControllerTest.abrir_comDuracaoInvalida_retorna400`, `SessaoTest.validarDuracao_comDuracaoInvalida_lancaExcecao` |
| AC-2.4 | Pauta inexistente → 404 | `SessaoControllerTest.abrir_comPautaInexistente_retorna404` |
| AC-2.5 | Sessão já aberta → 409 | `SessaoControllerTest.abrir_comSessaoJaAberta_retorna409` (Clock mockado, duas aberturas) |
| Should | Duração não inteira (`"abc"`, `1.5`) → 400 | `SessaoControllerTest.abrir_comDuracaoNaoInteira_retorna400` |

Decisões sobre NBSP/zero-width/emoji na fatia 1 não vêm do task.md — extrapolação da regra
"titulo obrigatório, não vazio". Ver Decisões.

## Módulos

- `domain.pauta`: `Pauta` (JPA, valida `titulo`/`descricao` no construtor), `PautaRepository`,
  `PautaInvalidaException`, `PautaNaoEncontradaException`.
- `domain.sessao`: `Sessao` (JPA, `@ManyToOne` pra `Pauta`, valida duração no construtor e via
  `validarDuracao` estático — chamado pelo service antes de buscar a pauta, pra manter a ordem
  400→404→409), `SessaoRepository`, `SessaoInvalidaException`, `SessaoJaAbertaException`.
- `application.pauta`: `PautaService` (criar, buscarPorId).
- `application.sessao`: `SessaoService` (abrir — recebe `Clock` injetado).
- `api.pauta` / `api.sessao`: Controllers, `*ExceptionHandler` escopados por controller, DTOs
  em `dto/`.
- `api`: `TamanhoMaximoBodyFilter` (filtro global, limite 16 KB), `CorpoExcedeTamanhoMaximoHandler`
  (advice global, fallback), `MetodoNaoSuportadoHandler` (advice global, 405).
- `infrastructure.persistence`: `PautaJpaRepository`/`PautaRepositoryImpl`,
  `SessaoJpaRepository`/`SessaoRepositoryImpl` (`findByPautaId` pra checar sessão aberta).
- `infrastructure.config`: `ClockConfig` (bean `Clock.systemUTC()`, substituído por
  `@MockBean Clock` nos testes), `JacksonConfig` (coerção customizada — ver Convenções).

## Stack e comandos

- Java 21, Spring Boot 3.3.4, Maven, H2 em memória (`ddl-auto: update`), sem Flyway.
- `mvnd test` — suite completa (70 testes, ~70s; boot de Spring Context em
  `PautaControllerTest`/`SessaoControllerTest` e Tomcat real em `CorpoExcedeTamanhoMaximoE2ETest`).
- Sem Docker/rede externa: nada de Testcontainers. Teste e2e de chunked usa
  `@SpringBootTest(webEnvironment = RANDOM_PORT)` (Tomcat embarcado) + `java.net.Socket` crudo.

## Convenções

- Regras de negócio no construtor das entidades (`Pauta`, `Sessao`), não em Bean Validation
  do DTO.
- **413 (fatia 1) tem três pontos de interceptação propositais:** (1) `PautaExceptionHandler`
  desembrulha a cadeia de causas de `HttpMessageNotReadableException` procurando
  `CorpoExcedeTamanhoMaximoException` — Jackson embrulha QUALQUER exceção do `InputStream`
  nesse tipo; (2) `CorpoExcedeTamanhoMaximoHandler` global como fallback pra rotas sem advice
  escopado (ex. `/pautas/{id}/sessao`); (3) catch em `TamanhoMaximoBodyFilter.doFilter` como
  última linha de defesa.
- `HttpRequestMethodNotSupportedException` (405) é lançada antes do Spring saber o controller
  → `MetodoNaoSuportadoHandler` é global, preenche `Allow` via `ex.getSupportedHttpMethods()`.
- Fatia 1: validação usa `isBlank()`/`strip()`/`codePointCount()` (não `trim()`/`length()`) e
  checagem de "caractere visível" por categoria Unicode — detalhes e gaps conhecidos em
  Decisões.
- Fatia 2: ordem de validação em `SessaoService.abrir` é corpo (400, `Sessao.validarDuracao`
  chamado ANTES de consultar `PautaRepository`) → pauta existe (404) → sessão aberta (409,
  checado iterando `sessaoRepository.buscarPorPautaId` e `Sessao.estaAberta(clock.instant())`).
  "Aberta" = `abertura.plus(duracaoMinutos, MINUTES).isAfter(agora)` — estritamente posterior,
  o instante exato do fim já conta como encerrada.
- **`JacksonConfig`:** por padrão Jackson trunca silenciosamente JSON float (`1.5`) pra
  `Integer` (`1`) sem erro — mascararia "duração não inteira" (AC should) como válida. Bean
  `Jackson2ObjectMapperBuilderCustomizer` força `CoercionAction.Fail` em `Float→Integer`
  globalmente. Faltava teste cobrindo esse caso quando o bug foi achado (NPE em
  `abertura e obrigatoria` porque o Clock mock só tinha stub pra 1 chamada e a duração
  truncada seguia adiante até o construtor de `Sessao`) — agora cobrido por
  `SessaoControllerTest.abrir_comDuracaoNaoInteira_retorna400`.
- `SessaoControllerTest` substitui o bean `Clock` real por `@MockBean Clock` — sem isso não dá
  pra testar AC-2.5 (duas aberturas, Clock precisa avançar determinística e controladamente
  entre elas) nem o cálculo de `fimEm` de forma previsível.

## Decisões

- Fatia 1: defaults sem resposta do PO (`titulo` obrigatório máx. 200, `descricao` opcional
  máx. 1000, erro `{status, mensagem}`). Título só com Zs/Zl/Zp/Cf/Cc/unassigned/surrogate é
  rejeitado como "titulo e obrigatorio" (pauta fantasma). **Gap conhecido:** checagem por
  categoria Unicode, não visual real — Hangul Filler U+3164, Braille Blank U+2800 passam como
  "visíveis" apesar de invisíveis. Decidir com PO se vale lista de exceção.
- Fatia 1: limite de body 16 KB via contagem real de bytes lidos (não só `Content-Length`),
  confirmado com e2e real contra Tomcat embarcado. HTTP/2 não testado.
- Fatia 2: código 409 (dúvida 1 do task.md) assumido conforme sugestão — não confirmado com
  PO. Reabertura após encerramento (dúvida 2) assumida fora de escopo: bloqueio só enquanto
  `estaAberta`; sessão encerrada nunca volta a contar (testado em
  `SessaoServiceTest.abrir_comSessaoEncerrada_abreNovaSessao` e
  `SessaoControllerTest.abrir_apoSessaoEncerrada_permiteNovaAbertura`). Sem limite máximo de
  duração (dúvida 3). `null`/ausente tratados igual, default 1 (dúvida 4).
- Fatia 2: `Sessao` sem cascata de remoção de `Pauta` (fora de escopo, conforme restrição
  técnica do task.md).
- Sem auth/autorização e sem ofuscação de id (`IDENTITY` sequencial) em nenhuma fatia:
  decisão registrada no README, não implementada. **Obrigatório revisitar antes da fatia 3**
  (IDOR por enumeração de id vira problema real com voto/resultado).

## Entregas

- Fatia 1/3 (pautas): AC-1.1–1.6, três ciclos de correção de review (handler de erro, Unicode,
  limite de body — ver commits anteriores para detalhe por ciclo). Suite: `PautaTest` (23),
  `PautaControllerTest` (14), `TamanhoMaximoBodyFilterTest` (6), `CorpoExcedeTamanhoMaximoE2ETest` (3).
- Fatia 2/3 (sessão), este ciclo: endpoint `POST /pautas/{id}/sessao`, entidade `Sessao` com
  `Clock` injetável, `SessaoService` com ordem 400→404→409, `JacksonConfig` pra duração não
  inteira. Suite: `SessaoTest` (10), `SessaoServiceTest` (6), `SessaoControllerTest` (8).
- Suite completa verde: `mvnd test` → 70 testes, 0 falhas.

## Armadilhas

- `mvn`/`mvnd` imprime warnings de `jline`/`jansi` (`UnsatisfiedLinkError`) — inofensivo.
- Testes com `@SpringBootTest` (Pauta/SessaoControllerTest, e2e) sobem contexto/Tomcat real;
  primeira execução de cada ~40s. Normal.
- MockMvc sempre deriva `Content-Length` do `byte[]` do payload — nunca produz chunked de
  verdade; e2e de chunked precisa de socket crudo contra servidor real.
- Jackson trunca float→Integer silenciosamente por padrão (ver `JacksonConfig` em
  Convenções) — qualquer novo campo `Integer`/`Long` em DTO que precise rejeitar decimal
  herda a proteção global, não precisa de coerção por campo.
- `@MockBean Clock` substitui o bean real do contexto inteiro do teste — todo request feito
  nesse `mockMvc` depois do primeiro `when(clock.instant())...thenReturn(...)` usa os valores
  stubados, não o relógio real; se o teste faz N requisições e só stuba 1 valor, a Nª chamada
  cai em `Mockito` default (`null` pra `Instant`) e vira NPE dentro do domínio — sempre stubar
  uma sequência (`thenReturn(T0).thenReturn(T0.plusSeconds(...))`) quando o teste faz mais de
  uma chamada que lê o clock.

## Pendências

- Fatia 3 (voto/resultado) não implementada. `GET /pautas` (listagem, Could) não implementado.
- Dúvidas do task.md (fatia 2: código 409, reabertura, limite de duração) seguem assumidas;
  confirmar com PO antes da fatia 3.
- Auth/id sequencial (ver Decisões): obrigatório revisitar antes da fatia 3, não depois.
- `spring-boot-starter-validation` no `pom.xml` sem uso — remover quando tocar no `pom.xml`.
- `PautaNaoEncontradaException(Long id)`: parâmetro `id` ignorado (mensagem hardcoded).
- "Título fantasma" (fatia 1) é checagem por categoria Unicode, não visual real — decisão de
  produto pendente sobre Hangul Filler/Braille Blank/etc.
- Fatia 2: sem encerramento automático por job (fora de escopo) — sessão "encerra" só por
  cálculo (`estaAberta` comparando com `clock.instant()` em tempo real), nunca por estado
  persistido. Se uma fatia futura precisar listar/filtrar sessões encerradas por consulta SQL
  direta (sem carregar e comparar em memória), vai precisar de uma coluna derivada ou view.
