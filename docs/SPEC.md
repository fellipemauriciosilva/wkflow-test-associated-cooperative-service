# Spec: API de votação em assembleias de cooperativa

**Status:** rascunho. Nenhum endpoint implementado neste estágio.

## Escopo

- Registrar votos de associados em pautas de assembleias de cooperativa.
- Fora do escopo desta versão: endpoints concretos (aguardam lista aprovada pelo solicitante), regras de quórum e pauta (aguardam definição).

## Modelo de ameaças

| Tema | Risco | Requisito mínimo |
|---|---|---|
| Autenticação do associado | Voto por pessoa não habilitada | Toda chamada de voto exige associado autenticado e habilitado para a assembleia. |
| Quórum | Decisão válida sem participação mínima | Regra de quórum definida antes da implementação; apuração só ocorre com quórum atingido. |
| Voto duplicado | Mais de um voto por associado na mesma pauta | Unicidade por (assembleia, pauta, associado), imposta no banco. |
| Sigilo | Voto atribuível ao associado | Persistência separa identidade do conteúdo do voto; resultados agregados apenas. |
| Integridade | Voto alterado após registro | Voto não é editável nem removível pela API após confirmado. |
| Auditabilidade | Impossível provar o que ocorreu | Registro de eventos (quem tentou votar, quando, resultado) sem expor o conteúdo do voto. |

## Critérios de aceite (rascunho)

- AC-S1: voto de associado não autenticado é rejeitado.
- AC-S2: segundo voto do mesmo associado na mesma pauta é rejeitado, sem alterar o primeiro.
- AC-S3: voto confirmado não pode ser alterado ou removido.
- AC-S4: resultado da apuração não expõe o voto individual de um associado.
- AC-S5: apuração sem quórum atingido não publica resultado.
- AC-S6: toda tentativa de voto gera registro de auditoria.

## Pendências

- Lista de endpoints e regras de votação (quórum, pauta, voto único).
- Prioridade de entrega.

## Fatia 1: Pautas (cadastro e consulta)

Fonte completa: `docs/TASK-fatia-1.md`. Cada AC abaixo é verificado pelo teste indicado no `README.md`.

- AC-1.1: POST /pautas com titulo válido retorna 201 com id. Teste: `PautaControllerTest#cadastraPautaComTituloERetornaId`.
- AC-1.2: GET /pautas/{id} retorna a pauta cadastrada, status 200. Teste: `PautaControllerTest#consultaPautaExistente`.
- AC-1.3: GET /pautas/{id} inexistente retorna 404 com erro NAO_ENCONTRADO. Teste: `PautaControllerTest#consultaPautaInexistenteRetorna404NoFormatoPadrao`.
- AC-1.4: POST /pautas sem titulo (ausente ou em branco) retorna 400 com erro VALIDACAO_INVALIDA e mensagem citando titulo. Testes: `PautaControllerTest#cadastroSemTituloRetorna400`, `PautaControllerTest#cadastroComTituloEmBrancoRetorna400`.
- AC-1.5: módulo sobe com Spring Boot 3, Java 21, H2 em memória e tabela PAUTA. Teste: `PautasApplicationTests#contextoSobeComH2EmMemoriaEComTabelaDePautas`.
- AC-1.6: erros 400 e 404 têm exatamente as chaves status, erro, mensagem, path. Teste: `PautaControllerTest#erros400e404TemExatamenteAsMesmasChaves`.

Verificação cruzada: os seis ACs acima batem com o texto de `docs/TASK-fatia-1.md`. Os testes de 405, 415, 406 e 500 cobrem o formato de erro de S1, mas não são ACs próprios.
