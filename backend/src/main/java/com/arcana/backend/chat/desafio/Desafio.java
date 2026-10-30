package com.arcana.backend.chat.desafio;

import com.arcana.backend.chat.dice.ModoRolagem;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Desafio {

    public static final int TENTATIVAS_MAXIMAS = 2;
    public static final int LIMITE_MENSAGENS = 30;

    private final String id;
    private final Long mestreId;
    private final int cd;
    private final String descricao;
    private final Instant criadoEm;
    private final Map<Long, Participacao> participacoes = new LinkedHashMap<>();
    private final List<MensagemDeResultado> mensagens = new ArrayList<>();

    public static class Participacao {

        private final Long jogadorId;
        private final String nomeJogador;
        private final String nomePersonagem;
        private ModoRolagem modo;
        private EstadoDesafio estado = EstadoDesafio.AGUARDANDO_ROLAGEM;
        private final List<RolagemDeDesafio> rolagens = new ArrayList<>();

        Participacao(Long jogadorId, String nomeJogador, String nomePersonagem, ModoRolagem modo) {
            this.jogadorId = jogadorId;
            this.nomeJogador = nomeJogador;
            this.nomePersonagem = nomePersonagem;
            this.modo = modo;
        }

        public Long getJogadorId() {
            return jogadorId;
        }

        public String getNomeJogador() {
            return nomeJogador;
        }

        public String getNomePersonagem() {
            return nomePersonagem;
        }

        public ModoRolagem getModo() {
            return modo;
        }

        public EstadoDesafio getEstado() {
            return estado;
        }

        public List<RolagemDeDesafio> getRolagens() {
            return List.copyOf(rolagens);
        }

        public int proximaTentativa() {
            return rolagens.size() + 1;
        }
    }

    public Desafio(String id, Long mestreId, int cd, String descricao, Instant criadoEm) {
        this.id = id;
        this.mestreId = mestreId;
        this.cd = cd;
        this.descricao = descricao;
        this.criadoEm = criadoEm;
    }

    void convocar(Long jogadorId, String nomeJogador, String nomePersonagem, ModoRolagem modo) {
        participacoes.put(jogadorId, new Participacao(jogadorId, nomeJogador, nomePersonagem, modo));
    }

    public String getId() {
        return id;
    }

    public Long getMestreId() {
        return mestreId;
    }

    public int getCd() {
        return cd;
    }

    public String getDescricao() {
        return descricao;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Collection<Participacao> participacoes() {
        return List.copyOf(participacoes.values());
    }

    public List<MensagemDeResultado> mensagensVisiveisPara(Long userId) {
        if (mestreId.equals(userId)) {
            return List.copyOf(mensagens);
        }
        return mensagens.stream().filter(m -> m.enviadaPara(userId)).toList();
    }

    public boolean envolve(Long userId) {
        return mestreId.equals(userId) || participacoes.containsKey(userId);
    }

    public Set<Long> destinatarios() {
        Set<Long> destinatarios = new LinkedHashSet<>();
        destinatarios.add(mestreId);
        destinatarios.addAll(participacoes.keySet());
        return destinatarios;
    }

    public boolean ativo() {
        return participacoes.values().stream().anyMatch(p -> p.estado.ativo());
    }

    public boolean jogadorAtivo(Long jogadorId) {
        Participacao participacao = participacoes.get(jogadorId);
        return participacao != null && participacao.estado.ativo();
    }

    public Participacao participacao(Long jogadorId) {
        Participacao participacao = participacoes.get(jogadorId);
        if (participacao == null) {
            throw new IllegalArgumentException("Este jogador não faz parte do desafio.");
        }
        return participacao;
    }

    void validarRolagem(Long jogadorId) {
        Participacao participacao = participacao(jogadorId);
        if (!participacao.estado.aguardandoRolagem()) {
            throw new IllegalStateException(switch (participacao.estado) {
                case AGUARDANDO_DECISAO_DO_MESTRE -> "Aguarde o mestre decidir se haverá uma nova tentativa.";
                case CANCELADO -> "Este desafio foi cancelado.";
                default -> "Este desafio já foi concluído.";
            });
        }
    }

    void registrarRolagem(Long jogadorId, RolagemDeDesafio rolagem) {
        validarRolagem(jogadorId);
        Participacao participacao = participacao(jogadorId);
        participacao.rolagens.add(rolagem);
        if (rolagem.sucesso()) {
            participacao.estado = EstadoDesafio.SUCESSO;
        } else if (participacao.rolagens.size() < TENTATIVAS_MAXIMAS) {
            participacao.estado = EstadoDesafio.AGUARDANDO_DECISAO_DO_MESTRE;
        } else {
            participacao.estado = EstadoDesafio.FALHA;
        }
    }

    void liberarTentativaExtra(Long jogadorId, ModoRolagem modo) {
        Participacao participacao = participacaoAguardandoDecisao(jogadorId);
        participacao.modo = modo;
        participacao.estado = EstadoDesafio.AGUARDANDO_TENTATIVA_EXTRA;
    }

    void encerrarComFalha(Long jogadorId) {
        participacaoAguardandoDecisao(jogadorId).estado = EstadoDesafio.FALHA;
    }

    boolean cancelar(Long jogadorId) {
        Participacao participacao = participacoes.get(jogadorId);
        if (participacao == null || !participacao.estado.ativo()) {
            return false;
        }
        participacao.estado = EstadoDesafio.CANCELADO;
        return true;
    }

    boolean cancelarTodos() {
        boolean algumCancelado = false;
        for (Long jogadorId : participacoes.keySet()) {
            algumCancelado |= cancelar(jogadorId);
        }
        return algumCancelado;
    }

    Set<Long> destinatariosDaMensagem(MensagemDeResultado.Destino destino, Long jogadorId) {
        Set<Long> destinatarios = switch (destino) {
            case JOGADOR -> {
                Participacao participacao = participacao(jogadorId);
                if (participacao.estado != EstadoDesafio.SUCESSO && participacao.estado != EstadoDesafio.FALHA) {
                    throw new IllegalStateException("Só é possível enviar a mensagem depois do resultado deste jogador.");
                }
                yield Set.of(jogadorId);
            }
            case VENCEDORES -> jogadoresNoEstado(EstadoDesafio.SUCESSO);
            case DERROTADOS -> jogadoresNoEstado(EstadoDesafio.FALHA);
        };
        if (destinatarios.isEmpty()) {
            throw new IllegalStateException(destino == MensagemDeResultado.Destino.VENCEDORES
                    ? "Nenhum jogador venceu este desafio ainda."
                    : "Nenhum jogador falhou neste desafio ainda.");
        }
        return destinatarios;
    }

    void registrarMensagem(MensagemDeResultado mensagem) {
        if (mensagens.size() >= LIMITE_MENSAGENS) {
            throw new IllegalStateException("Este desafio já recebeu o máximo de " + LIMITE_MENSAGENS + " mensagens.");
        }
        mensagens.add(mensagem);
    }

    private Set<Long> jogadoresNoEstado(EstadoDesafio estado) {
        Set<Long> jogadores = new LinkedHashSet<>();
        participacoes.values().stream()
                .filter(p -> p.estado == estado)
                .forEach(p -> jogadores.add(p.jogadorId));
        return jogadores;
    }

    private Participacao participacaoAguardandoDecisao(Long jogadorId) {
        Participacao participacao = participacao(jogadorId);
        if (participacao.estado != EstadoDesafio.AGUARDANDO_DECISAO_DO_MESTRE) {
            throw new IllegalStateException("Só é possível decidir a tentativa extra depois de uma primeira rolagem com falha.");
        }
        return participacao;
    }
}
