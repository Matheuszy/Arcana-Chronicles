package com.arcana.backend.character.service;

import com.arcana.backend.character.PacoteEquipamentoDto;
import com.arcana.backend.character.PersonagemResponseDto;
import com.arcana.backend.character.model.Personagem;
import com.arcana.backend.character.repository.PacoteEquipamentoRepository;
import com.arcana.backend.character.repository.PersonagemRepository;
import com.arcana.backend.config.exception.RecursoNaoEncontradoException;
import com.arcana.backend.config.exception.RegraDeNegocioException;
import com.arcana.backend.table.model.StatusMesa;
import com.arcana.backend.table.model.StatusSolicitacao;
import com.arcana.backend.table.repository.MesaRepository;
import com.arcana.backend.table.repository.SolicitacaoEntradaRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PacoteEquipamentoService {

    private final PacoteEquipamentoRepository pacoteRepository;
    private final PersonagemRepository personagemRepository;
    private final MesaRepository mesaRepository;
    private final SolicitacaoEntradaRepository solicitacaoRepository;

    public PacoteEquipamentoService(PacoteEquipamentoRepository pacoteRepository,
                                    PersonagemRepository personagemRepository,
                                    MesaRepository mesaRepository,
                                    SolicitacaoEntradaRepository solicitacaoRepository) {
        this.pacoteRepository = pacoteRepository;
        this.personagemRepository = personagemRepository;
        this.mesaRepository = mesaRepository;
        this.solicitacaoRepository = solicitacaoRepository;
    }

    @Transactional(readOnly = true)
    public List<PacoteEquipamentoDto> listar() {
        return pacoteRepository.findAllByOrderByPosicaoAsc().stream().map(PacoteEquipamentoDto::from).toList();
    }

    @Transactional
    public PersonagemResponseDto aplicar(Long personagemId, Long donoId, String chave) {
        Personagem personagem = personagemRepository.findById(personagemId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Personagem não encontrado: " + personagemId));
        if (!personagem.getOwner().getId().equals(donoId)) {
            throw new AccessDeniedException("Personagem não pertence a este usuário.");
        }
        if (vinculadaAMesa(personagem)) {
            throw new RegraDeNegocioException(
                    "A ficha está vinculada a uma mesa. Saia da mesa para aplicar ou trocar o pacote de equipamento.");
        }
        String chaveNormalizada = chave.trim().toUpperCase();
        personagem.aplicarPacote(pacoteRepository.findById(chaveNormalizada)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Pacote de equipamento não encontrado: " + chaveNormalizada)));
        return PersonagemResponseDto.from(personagemRepository.save(personagem));
    }

    private boolean vinculadaAMesa(Personagem personagem) {
        Long personagemId = personagem.getId();
        Long donoId = personagem.getOwner().getId();
        boolean participante = mesaRepository.findByParticipantsCharacterId(personagemId).stream()
                .filter(mesa -> mesa.getStatus() != StatusMesa.ENCERRADA)
                .anyMatch(mesa -> mesa.getParticipants().stream().anyMatch(p ->
                        personagemId.equals(p.getCharacterId()) && donoId.equals(p.getUserId())));
        return participante
                || solicitacaoRepository.existsByCharacterIdAndStatus(personagemId, StatusSolicitacao.PENDENTE);
    }
}
