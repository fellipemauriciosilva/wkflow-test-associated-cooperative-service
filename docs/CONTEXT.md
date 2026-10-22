# Contexto do projeto

## Módulos

- `com.acme.votacao` — classe main (`VotacaoPautasApplication`).
- `com.acme.votacao.pauta` — pacote único, Fatias 1+2+3:
  - Fatia 1: `Pauta`/`PautaRepository`/`PautaRequest`/`PautaResponse`/`PautaIdResponse`/`PautaService`/`PautaController`/`PautaNaoEncontradaException`.
  - Fatia 2: `Sessao` (entidade JPA), `SessaoRepository`, `SessaoRequest`/`SessaoResponse` (DTOs), `SessaoService` (abertura com lock pessimista + isolamento fixado), `SessaoController` (`POST /pautas/{id}/sessao`), `SessaoJaAbertaException`, `ClockConfig` (bean `Clock.systemUTC()`), `GanchoConcorrenciaSessao`/`GanchoConcorrenciaSessaoNoOp`.
  - Fatia 3: `Voto` (entidade JPA, constraint `UNIQUE(pauta_id, associado_id)`), `VotoValor` (enum SIM/NAO), `VotoRepository`, `VotoRequest`/`VotoResponse` (DTOs), `VotoService` (ordem 404→422→409), `VotoController` (`POST /pautas/{id}/votos`), `VotoDuplicadoException`, `SessaoNaoAbertaException`, `GanchoConcorrenciaVoto`/`GanchoConcorrenciaVotoNoOp` (barreira de teste, análoga à da Fatia 2); `StatusVotacao` (enum ABERTA/ENCERRADA/NAO_INICIADA), `ResultadoVotacaoResponse`, `ResultadoVotacaoService`, `ResultadoVotacaoController` (`GET /pautas/{id}/resultado`).
  - Compartilhado pelas três fatias: `ErroResponse` (corpo padronizado de erro) e `GlobalExceptionHandler`, cobrindo 400/404/409/422/415/405/406/404-de-rota/500. `GlobalExceptionHandler` estende `ResponseEntityExceptionHandler` (não é um `@RestControllerAdvice` solto com catch-all) — ver Decisões.

## Stack e comandos

- Java 21, Spring Boot 3.3.4 (web, data-jpa, validation), H2 em memória, Maven.
- Build/teste: `mvnd test` (sem `clean`). Build isolado: `mvnd compile`.
- Rodar local: `mvnd spring-boot:run`. Com console H2: `mvnd spring-boot:run -Dspring-boot.run.profiles=dev`.
- H2: `jdbc:h2:mem:votacao`, `ddl-auto: update`, sem migração (Flyway não usado — fora de escopo para greenfield pequeno com H2 em memória).

## Convenções

- DTOs como `record` (Java 21), sem Lombok (não adicionado ao pom). Enums simples (`VotoValor`, `StatusVotacao`) sem lógica.
- Validação via Bean Validation (`@NotBlank`/`@Size(max=255)` em `PautaRequest`; `@Positive` + `@Max(1440)` em `SessaoRequest.duracaoMinutos`; `@NotBlank`/`@NotNull` em `VotoRequest`). `GlobalExceptionHandler` trata dois grupos: (1) tudo que `ResponseEntityExceptionHandler` já sabe mapear para um `HttpStatusCode` (`MethodArgumentNotValidException`/`HttpMessageNotReadableException`/`TypeMismatchException`/etc., inclui valor de enum inválido em JSON, que vira `HttpMessageNotReadableException` → 400) via `handleExceptionInternal` sobrescrito, preservando o status original; (2) exceções próprias do domínio via `@ExceptionHandler` dedicado: `PautaNaoEncontradaException` → 404, `SessaoJaAbertaException`/`VotoDuplicadoException` → 409, `SessaoNaoAbertaException` → 422, `Exception` genérico → 500. Corpo de erro é `ErroResponse` (`timestamp`, `status`, `error`, `message`, `path`) para **todos** os handlers (RF-12).
- Handlers de 400 nunca usam `ex.getMessage()` bruto (ver Fatia 2). Status 5xx usa mensagem fixa; demais usam `status.getReasonPhrase()` como fallback.
- `Clock` é bean de produção (`ClockConfig`, `Clock.systemUTC()`), nunca `Instant.now()` direto em código de produção. `VotoService`/`ResultadoVotacaoService` seguem o mesmo padrão de `SessaoService`.
- `spring.jackson.deserialization.accept-float-as-int: false` em `application.yml` (ver Fatia 2, Armadilhas).
- Pacote único `pauta` (sem camadas `api/application/domain/infrastructure`).
- Ordem de validação do voto (`VotoService.votar`, fixada no task.md): 400 (payload, via `@Valid` no controller, antes do serviço) → 404 (pauta) → 422 (sessão não aberta) → 409 (duplicado). Checagem de duplicado é otimista (`existsByPautaIdAndAssociadoId`) **e** reforçada por `catch (DataIntegrityViolationException)` no `save` — ver Decisões.
- "Sessão aberta" para voto usa a mesma regra da Fatia 2 (`findFirstByPautaIdAndFimAfter`, `fim > agora` estrito — fronteira `agora == fim` é fechada). "Status da votação" (resultado) usa a **última** sessão da pauta (`findFirstByPautaIdOrderByFimDesc`), não "existe alguma aberta": `agora < fim` → ABERTA, senão ENCERRADA; sem nenhuma sessão → NAO_INICIADA.

## Decisões

- (Fatia 2) `GlobalExceptionHandler` estende `ResponseEntityExceptionHandler`; ver histórico de commits anteriores para o racional completo (catch-all genérico convertia 4xx do Spring MVC em 500).
- (Fatia 2) Concorrência na abertura de sessão via lock pessimista + isolamento READ_COMMITTED fixado; ver histórico de commits e README para o racional completo.
- (Fatia 3) Voto duplicado sob concorrência: duas camadas de defesa. (1) Checagem otimista em `VotoService.votar` (`existsByPautaIdAndAssociadoId`) evita round-trip ao banco no caminho feliz (99% dos casos não têm corrida). (2) A constraint `UNIQUE(pauta_id, associado_id)` do H2 é quem garante corretude sob concorrência real — `DataIntegrityViolationException` do `save` é capturada e convertida em `VotoDuplicadoException` (409). Diferente da Fatia 2 (lock pessimista, sem constraint de unicidade possível porque reabertura é permitida), aqui a dupla de "pauta+associado" É sempre única — constraint de banco é a ferramenta certa, não lock.
- (Fatia 3) Prova de concorrência determinística para voto: `GanchoConcorrenciaVoto`/`GanchoConcorrenciaVotoNoOp` (ponto de extensão, chamado em `VotoService.votar` depois da checagem otimista e antes do `save`). Diferente do gancho de sessão (retém a 1ª thread indefinidamente até liberação explícita do teste), este é uma barreira simétrica de 2 partes (`CountDownLatch(2)`): ambas as chamadas concorrentes avançam até o `antesDeSalvar`, aguardam uma à outra, e só então os dois `save` disparam quase simultaneamente — é isso que exercita a constraint do banco de fato, não só a checagem otimista. Desarmado por padrão (`armado = false`, passa direto) via `ativarBarreira()`: como o bean é singleton por classe de teste, uma barreira sempre ativa bloquearia qualquer chamada de voto fora do próprio teste de concorrência.
- (Fatia 3) `NAO_INICIADA` (resultado de pauta sem sessão) e desambiguação sessão-inexistente-vs-pauta-inexistente (422 vs 404) implementados conforme task.md seção 4 — ambos marcados como "assumido, confirmar com PO" no próprio task.md; ver Pendências.
- (Fatia 3) Existência do `associadoId` não é validada (string livre, só `@NotBlank`) — conforme task.md, fora de escopo desta fatia.

## Entregas

- Fatia 1 e Fatia 2 (incluindo 2 ciclos de correção pós-review): ver histórico de commits anteriores a este. Resumo: `POST/GET /pautas`, `POST /pautas/{id}/sessao`, corpo de erro padronizado RF-12, 43 testes (`mvnd test` → BUILD SUCCESS).
- Fatia 3 (voto e resultado), primeira implementação:
  - `POST /pautas/{id}/votos`: 201/400/404/409/422, AC-3.1, AC-3.2, AC-3.3, AC-3.4, AC-3.6, AC-3.8, AC-3.9, AC-3.10 cobertos em `VotoControllerTest` (12 testes MockMvc + H2 real).
  - `GET /pautas/{id}/resultado`: 200/404, AC-3.5, AC-3.6, AC-3.7 cobertos em `ResultadoVotacaoControllerTest` (4 testes).
  - `VotoServiceTest` (5 testes JUnit+Mockito): registro feliz, pauta inexistente, sessão não aberta, duplicado otimista, duplicado via `DataIntegrityViolationException`.
  - `ResultadoVotacaoServiceTest` (5 testes): pauta inexistente, sem sessão (NAO_INICIADA), aberta, fronteira `agora==fim` (ENCERRADA), depois do fim (ENCERRADA).
  - Teste de concorrência determinístico (AC-3.10) via `GanchoConcorrenciaVotoControlavel`, mesmo padrão da Fatia 2 adaptado para barreira simétrica (ver Decisões).
  - Suite completa: 69 testes (43 Fatias 1+2 + 26 Fatia 3), `mvnd test` → BUILD SUCCESS (confirmado nesta sessão).

## Armadilhas

- (Fatia 2) `mvnd`/`mvn` resolvem via proxy interno; `@Size(max=255)` sem `@Column(length=255)`; contaminação de teste entre métodos da mesma classe `@SpringBootTest` (usar `countByXxx` escopado, nunca `count()` global); gancho de concorrência precisa estar depois do ponto de contenção real e antes do commit; Jackson trunca float para int por padrão. Ver histórico de commits para detalhe completo.
- (Fatia 3) Mesma armadilha de contaminação entre métodos se repete em `VotoControllerTest`/`ResultadoVotacaoControllerTest`: `VotoRepository` ganhou `countByPautaId(Long)` só para isso, nunca usar `votoRepository.count()` em assert de "nenhum voto criado".
- (Fatia 3) Gancho de concorrência de voto (`GanchoConcorrenciaVotoControlavel`) **precisa** ser desarmável: diferente do gancho de sessão (um teste só, sempre libera no fim), aqui o mesmo bean singleton atende todos os métodos de `VotoControllerTest`; sem o `armado.set(false)` após a barreira passar e sem default "passa direto", qualquer chamada de voto em outro teste da mesma classe trava esperando uma segunda chamada que nunca chega.
- (Fatia 3) Valor de voto fora de SIM/NAO no JSON (ex. `"TALVEZ"`) falha na desserialização do enum *antes* do Bean Validation rodar — vira `HttpMessageNotReadableException`, já coberta pelo `handleExceptionInternal` existente (sem precisar de handler novo).

## Pendências

- Itens "assumido, confirmar com PO" da tabela da seção 4 do task.md da Fatia 3, ainda não confirmados: origem do `associadoId` no body, não validar existência do associado, resultado parcial com `status=ABERTA`, voto fora da sessão → 422, formato do resultado, e especialmente `NAO_INICIADA` (marcado como "**Novo, não existe na master**" no próprio task.md) e a desambiguação sessão-inexistente(422)/pauta-inexistente(404).
- RF-13 (listagem paginada), RF-14 (percentuais no resultado), empate/vencedor explícito: fora de escopo em todas as fatias (Could/Won't).
- Teto de `duracaoMinutos` fixado em 1440 (24h) — provisório, não confirmado com o PO (herdado da Fatia 2).
- Confirmar com PO o limite de 255 code units (UTF-16) em `titulo`/`descricao` (herdado da Fatia 1).
- Itens de segurança fora de escopo, revisitar antes de deploy real: limite de tamanho de corpo de requisição, auth/rate-limit em todos os endpoints de escrita, trim de espaços em strings de entrada (`titulo`/`descricao`/`associadoId`), ids sequenciais enumeráveis (IDOR).
- Itens não bloqueantes, fora deste merge: índice `(pauta_id, fim)` e FK em `sessao.pauta_id`; índice em `voto.pauta_id` (sem índice, `countByPautaIdAndVoto` e `existsByPautaIdAndAssociadoId` ficam mais lentos conforme a tabela cresce) e FK em `voto.pauta_id`; centralizar `@Import(TestClockConfig.class)` numa base class de teste.
