package com.arcana.backend.chat.desafio;

import com.arcana.backend.chat.dice.ModoRolagem;

import java.time.Instant;
import java.util.List;
import java.util.Set;

public record DesafioDto(
        String id,
        Long criadorId,
        String criadorNome,
        boolean doMestre,
        boolean doCombate,
        Integer cd,
        String descricao,
        Instant criadoEm,
        boolean ativo,
        List<ParticipacaoDto> jogadores,
        List<MensagemDto> mensagens
) {

    public record ParticipacaoDto(
            String id,
            Desafio.TipoAlvo tipo,
            Long jogadorId,
            String nomeJogador,
            String nomePersonagem,
            ModoRolagem modo,
            EstadoDesafio estado,
            ModoRolagem modoEfetivo,
            List<RolagemDeDesafio> rolagens,
            List<Ajuda> ajudas
    ) {}

    public record MensagemDto(
            String id,
            String texto,
            MensagemDeResultado.Destino destino,
            Set<Long> destinatarios,
            Instant enviadaEm
    ) {

        static MensagemDto paraUsuario(MensagemDeResultado mensagem, boolean ehCriador) {
            return new MensagemDto(mensagem.id(), mensagem.texto(), mensagem.destino(),
                    ehCriador ? mensagem.destinatarios() : null, mensagem.enviadaEm());
        }
    }

    public static DesafioDto paraUsuario(Desafio desafio, Long userId) {
        boolean ehCriador = desafio.getCriadorId().equals(userId);
        boolean envolvido = desafio.envolve(userId);
        return new DesafioDto(
                desafio.getId(),
                desafio.getCriadorId(),
                desafio.getNomeDoCriador(),
                desafio.isDoMestre(),
                desafio.isDoCombate(),
                envolvido ? desafio.getCd() : null,
                desafio.getDescricao(),
                desafio.getCriadoEm(),
                desafio.ativo(),
                desafio.participacoes().stream()
                        .map(p -> new ParticipacaoDto(p.getId(), p.getTipo(), p.getControladorId(), p.getNomeJogador(), p.getNomePersonagem(),
                                p.getModo(), p.getEstado(), desafio.modoEfetivo(p.getId()), p.getRolagens(),
                                p.getAjudas()))
                        .toList(),
                desafio.mensagensVisiveisPara(userId).stream()
                        .map(m -> MensagemDto.paraUsuario(m, ehCriador))
                        .toList());
    }

    public static DesafioDto paraMestre(Desafio desafio) {
        return paraUsuario(desafio, desafio.getCriadorId());
    }
}
