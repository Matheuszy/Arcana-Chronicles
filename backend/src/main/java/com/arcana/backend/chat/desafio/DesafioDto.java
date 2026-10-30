package com.arcana.backend.chat.desafio;

import com.arcana.backend.chat.dice.ModoRolagem;

import java.time.Instant;
import java.util.List;
import java.util.Set;

public record DesafioDto(
        String id,
        int cd,
        String descricao,
        Instant criadoEm,
        boolean ativo,
        List<ParticipacaoDto> jogadores,
        List<MensagemDto> mensagens
) {

    public record ParticipacaoDto(
            Long jogadorId,
            String nomeJogador,
            String nomePersonagem,
            ModoRolagem modo,
            EstadoDesafio estado,
            List<RolagemDeDesafio> rolagens
    ) {}

    public record MensagemDto(
            String id,
            String texto,
            MensagemDeResultado.Destino destino,
            Set<Long> destinatarios,
            Instant enviadaEm
    ) {

        static MensagemDto paraUsuario(MensagemDeResultado mensagem, boolean ehMestre) {
            return new MensagemDto(mensagem.id(), mensagem.texto(), mensagem.destino(),
                    ehMestre ? mensagem.destinatarios() : null, mensagem.enviadaEm());
        }
    }

    public static DesafioDto paraUsuario(Desafio desafio, Long userId) {
        boolean ehMestre = desafio.getMestreId().equals(userId);
        return new DesafioDto(
                desafio.getId(),
                desafio.getCd(),
                desafio.getDescricao(),
                desafio.getCriadoEm(),
                desafio.ativo(),
                desafio.participacoes().stream()
                        .map(p -> new ParticipacaoDto(p.getJogadorId(), p.getNomeJogador(), p.getNomePersonagem(),
                                p.getModo(), p.getEstado(), p.getRolagens()))
                        .toList(),
                desafio.mensagensVisiveisPara(userId).stream()
                        .map(m -> MensagemDto.paraUsuario(m, ehMestre))
                        .toList());
    }

    public static DesafioDto paraMestre(Desafio desafio) {
        return paraUsuario(desafio, desafio.getMestreId());
    }
}
