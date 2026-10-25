package com.acme.pautas.service;

import com.acme.pautas.domain.OpcaoVoto;
import com.acme.pautas.domain.Voto;
import com.acme.pautas.repository.PautaRepository;
import com.acme.pautas.repository.SessaoRepository;
import com.acme.pautas.repository.VotoRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;

@Service
public class VotoService {

    private static final String CONSTRAINT_UNICA_VOTO = "UK_VOTO_PAUTA_ASSOCIADO";
    private static final int TAMANHO_MAXIMO_ASSOCIADO = 255;

    private final PautaRepository pautas;
    private final SessaoRepository sessoes;
    private final VotoRepository votos;
    private final Clock clock;

    public VotoService(PautaRepository pautas, SessaoRepository sessoes, VotoRepository votos, Clock clock) {
        this.pautas = pautas;
        this.sessoes = sessoes;
        this.votos = votos;
        this.clock = clock;
    }

    @Transactional
    public Voto registrar(Long pautaId, String associadoId, OpcaoVoto opcao) {
        String associado = normalizar(associadoId);
        pautas.findWithLockById(pautaId).orElseThrow(() -> new PautaNaoEncontradaException(pautaId));
        if (!sessoes.existsByPautaIdAndFimAfter(pautaId, Instant.now(clock))) {
            throw new SemSessaoAbertaException(pautaId);
        }
        if (votos.existsByPautaIdAndAssociadoId(pautaId, associado)) {
            throw new VotoDuplicadoException(pautaId, associado);
        }
        try {
            return votos.saveAndFlush(new Voto(pautaId, associado, opcao));
        } catch (DataIntegrityViolationException e) {
            if (violaConstraintUnicaDoVoto(e)) {
                throw new VotoDuplicadoException(pautaId, associado, e);
            }
            throw e;
        }
    }

    private static String normalizar(String associadoId) {
        String normalizado = Normalizer.normalize(associadoId.strip(), Normalizer.Form.NFC);
        if (normalizado.codePointCount(0, normalizado.length()) > TAMANHO_MAXIMO_ASSOCIADO) {
            throw new AssociadoInvalidoException("associadoId: deve ter no máximo 255 caracteres");
        }
        return normalizado;
    }

    private static boolean violaConstraintUnicaDoVoto(Throwable erro) {
        for (Throwable atual = erro; atual != null; atual = atual.getCause()) {
            if (atual instanceof ConstraintViolationException violacao
                    && violacao.getConstraintName() != null
                    && violacao.getConstraintName().toUpperCase(Locale.ROOT).contains(CONSTRAINT_UNICA_VOTO)) {
                return true;
            }
        }
        return false;
    }

    @Transactional(readOnly = true)
    public ResultadoVotacao resultado(Long pautaId) {
        if (!pautas.existsById(pautaId)) {
            throw new PautaNaoEncontradaException(pautaId);
        }
        long sim = 0;
        long nao = 0;
        for (Object[] linha : votos.contarPorOpcao(pautaId)) {
            OpcaoVoto opcao = (OpcaoVoto) linha[0];
            long total = (Long) linha[1];
            if (opcao == OpcaoVoto.SIM) {
                sim = total;
            } else {
                nao = total;
            }
        }
        return new ResultadoVotacao(sim, nao);
    }
}
