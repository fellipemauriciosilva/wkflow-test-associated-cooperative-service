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
