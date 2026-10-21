package com.votacao.application.voto;

import com.votacao.application.pauta.PautaService;
import com.votacao.domain.pauta.Pauta;
import com.votacao.domain.sessao.Sessao;
import com.votacao.domain.sessao.SessaoRepository;
import com.votacao.domain.voto.SessaoIndisponivelParaVotoException;
import com.votacao.domain.voto.TipoVoto;
import com.votacao.domain.voto.Voto;
import com.votacao.domain.voto.VotoDuplicadoException;
import com.votacao.domain.voto.VotoRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VotoService {

    private final VotoRepository votoRepository;
    private final SessaoRepository sessaoRepository;
    private final PautaService pautaService;
    private final Clock clock;

    public VotoService(
            VotoRepository votoRepository,
            SessaoRepository sessaoRepository,
            PautaService pautaService,
            Clock clock) {
        this.votoRepository = votoRepository;
        this.sessaoRepository = sessaoRepository;
        this.pautaService = pautaService;
        this.clock = clock;
    }

    @Transactional
    public Voto registrar(Long pautaId, Long idAssociado, TipoVoto tipo) {
        Pauta pauta = pautaService.buscarPorId(pautaId);

        validarSessaoAberta(pautaId);

        if (votoRepository.existsByPautaIdAndIdAssociado(pautaId, idAssociado)) {
            throw new VotoDuplicadoException(idAssociado, pautaId);
        }

        Voto voto = new Voto(pauta, idAssociado, tipo);

        try {
            return votoRepository.save(voto);
        } catch (DataIntegrityViolationException ex) {
            if (violaConstraintDeVotoUnico(ex)) {
                throw new VotoDuplicadoException(idAssociado, pautaId);
            }
            throw ex;
        }
    }

    private void validarSessaoAberta(Long pautaId) {
        Optional<Sessao> sessao = sessaoRepository.findByPautaId(pautaId);
        LocalDateTime agora = LocalDateTime.now(clock);

        boolean aberta = sessao.isPresent()
                && !agora.isBefore(sessao.get().getAbertura())
                && agora.isBefore(sessao.get().getFechamento());

        if (!aberta) {
            throw new SessaoIndisponivelParaVotoException(pautaId);
        }
    }

    private boolean violaConstraintDeVotoUnico(DataIntegrityViolationException ex) {
        String mensagem = String.valueOf(ex.getMostSpecificCause().getMessage()).toLowerCase(Locale.ROOT);
        boolean nomeDaConstraint = mensagem.contains(Voto.UK_PAUTA_ASSOCIADO.toLowerCase(Locale.ROOT));
        boolean tabelaEColunas = mensagem.contains("voto")
                && mensagem.contains("pauta_id")
                && mensagem.contains("id_associado");
        return nomeDaConstraint || tabelaEColunas;
    }
}
