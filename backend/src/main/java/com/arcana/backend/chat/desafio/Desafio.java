package com.arcana.backend.chat.desafio;

import com.arcana.backend.chat.dice.ModoRolagem;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Desafio {

    public static final int TENTATIVAS_MAXIMAS = 2;
    public static final int LIMITE_MENSAGENS = 30;

    public enum TipoAlvo {
        JOGADOR,
        MONSTRO
    }

    private final String id;
    private final Long criadorId;
    private final String nomeDoCriador;
    private final boolean doMestre;
    private final String combateId;
    private final int cd;
    private final String descricao;
    private final Instant criadoEm;
    private final Map<String, Participacao> participacoes = new LinkedHashMap<>();
    private final List<MensagemDeResultado> mensagens = new ArrayList<>();

    public static class Participacao {

        private final String id;
        private final TipoAlvo tipo;
        private final Long controladorId;
        private final Long personagemId;
        private final String nomeJogador;
        private final String nomePersonagem;
        private ModoRolagem modo;
        private EstadoDesafio estado = EstadoDesafio.AGUARDANDO_ROLAGEM;
        private final List<RolagemDeDesafio> rolagens = new ArrayList<>();
        private final List<Ajuda> ajudas = new ArrayList<>();
        private final Set<Long> autoresDaTentativa = new HashSet<>();

        Participacao(String id, TipoAlvo tipo, Long controladorId, Long personagemId, String nomeJogador,
                     String nomePersonagem, ModoRolagem modo) {
            this.id = id;
            this.tipo = tipo;
            this.controladorId = controladorId;
            this.personagemId = personagemId;
            this.nomeJogador = nomeJogador;
            this.nomePersonagem = nomePersonagem;
            this.modo = modo;
        }

        public String getId() {
            return id;
        }

        public TipoAlvo getTipo() {
            return tipo;
        }

        public Long getControladorId() {
            return controladorId;
        }

        public Long getPersonagemId() {
            return personagemId;
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

        public List<Ajuda> getAjudas() {
            return List.copyOf(ajudas);
        }

        public int proximaTentativa() {
            return rolagens.size() + 1;
        }

        public void definirModo(ModoRolagem novoModo) {
            this.modo = novoModo;
        }
    }

    public Desafio(String id, Long criadorId, String nomeDoCriador, boolean doMestre, String combateId, int cd,
                   String descricao, Instant criadoEm) {
        this.id = id;
        this.criadorId = criadorId;
        this.nomeDoCriador = nomeDoCriador;
        this.doMestre = doMestre;
        this.combateId = combateId;
        this.cd = cd;
        this.descricao = descricao;
        this.criadoEm = criadoEm;
    }

    void convocarJogador(Long jogadorId, String nomeJogador, String nomePersonagem, ModoRolagem modo) {
        String alvoId = idDoJogador(jogadorId);
        participacoes.put(alvoId, new Participacao(alvoId, TipoAlvo.JOGADOR, jogadorId, null, nomeJogador,
                nomePersonagem, modo));
    }

    void convocarMonstro(String alvoId, Long mestreId, Long personagemId, String nomeMestre, String nomeMonstro) {
        participacoes.put(alvoId, new Participacao(alvoId, TipoAlvo.MONSTRO, mestreId, personagemId, nomeMestre,
                nomeMonstro, ModoRolagem.NORMAL));
    }

    public static String idDoJogador(Long jogadorId) {
        return String.valueOf(jogadorId);
    }

    public String getId() {
        return id;
    }

    public Long getCriadorId() {
        return criadorId;
    }

    public String getNomeDoCriador() {
        return nomeDoCriador;
    }

    public boolean isDoMestre() {
        return doMestre;
    }

    public boolean isDoCombate() {
        return combateId != null;
    }

    public String getCombateId() {
        return combateId;
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
        if (userId == null) {
            return List.of();
        }
        if (criadorId.equals(userId)) {
            return List.copyOf(mensagens);
        }
        return mensagens.stream().filter(m -> m.enviadaPara(userId)).toList();
    }

    public boolean envolve(Long userId) {
        return criadorId.equals(userId)
                || participacoes.values().stream().anyMatch(p -> p.controladorId.equals(userId));
    }

    public Set<Long> destinatarios() {
        Set<Long> destinatarios = new LinkedHashSet<>();
        destinatarios.add(criadorId);
        participacoes.values().forEach(p -> destinatarios.add(p.controladorId));
        return destinatarios;
    }

    public boolean ativo() {
        return participacoes.values().stream().anyMatch(p -> p.estado.ativo());
    }

    public boolean alvoAtivo(String alvoId) {
        Participacao participacao = participacoes.get(alvoId);
        return participacao != null && participacao.estado.ativo();
    }

    public boolean jogadorAtivo(Long jogadorId) {
        Participacao participacao = participacoes.get(idDoJogador(jogadorId));
        return participacao != null && participacao.tipo == TipoAlvo.JOGADOR && participacao.estado.ativo();
    }

    public Participacao participacao(String alvoId) {
        Participacao participacao = participacoes.get(alvoId);
        if (participacao == null) {
            throw new IllegalArgumentException("Este jogador não faz parte do desafio.");
        }
        return participacao;
    }

    void validarRolagem(String alvoId) {
        Participacao participacao = participacao(alvoId);
        if (!participacao.estado.aguardandoRolagem()) {
            throw new IllegalStateException(switch (participacao.estado) {
                case AGUARDANDO_DECISAO_DO_MESTRE -> "Aguarde "
                        + (doMestre ? "o mestre" : nomeDoCriador) + " decidir se haverá uma nova tentativa.";
                case AGUARDANDO_CONFIRMACAO -> "Aguarde "
                        + (doMestre ? "o mestre" : nomeDoCriador) + " confirmar o resultado.";
                case CANCELADO -> "Este desafio foi cancelado.";
                default -> "Este desafio já foi concluído.";
            });
        }
    }

    void registrarRolagem(String alvoId, RolagemDeDesafio rolagem) {
        validarRolagem(alvoId);
        Participacao participacao = participacao(alvoId);
        participacao.rolagens.add(rolagem);
        participacao.estado = EstadoDesafio.AGUARDANDO_CONFIRMACAO;
    }

    public ModoRolagem modoEfetivo(String alvoId) {
        Participacao participacao = participacao(alvoId);
        return combinarModos(participacao.modo, participacao.ajudas);
    }

    static ModoRolagem combinarModos(ModoRolagem base, List<Ajuda> ajudas) {
        boolean vantagem = base == ModoRolagem.VANTAGEM
                || ajudas.stream().anyMatch(ajuda -> ajuda.tipo() == TipoAjuda.VANTAGEM);
        boolean anulada = ajudas.stream().anyMatch(ajuda -> ajuda.tipo() == TipoAjuda.ANULAR_DESVANTAGEM);
        boolean desvantagem = !anulada && (base == ModoRolagem.DESVANTAGEM
                || ajudas.stream().anyMatch(ajuda -> ajuda.tipo() == TipoAjuda.DESVANTAGEM));
        if (vantagem == desvantagem) {
            return ModoRolagem.NORMAL;
        }
        return vantagem ? ModoRolagem.VANTAGEM : ModoRolagem.DESVANTAGEM;
    }

    boolean confirmar(String alvoId) {
        Participacao participacao = participacao(alvoId);
        if (participacao.estado != EstadoDesafio.AGUARDANDO_CONFIRMACAO) {
            return false;
        }
        int ultima = participacao.rolagens.size() - 1;
        RolagemDeDesafio confirmada = participacao.rolagens.get(ultima).confirmar(List.copyOf(participacao.ajudas), cd);
        participacao.rolagens.set(ultima, confirmada);
        participacao.ajudas.clear();
        participacao.autoresDaTentativa.clear();
        if (confirmada.sucesso()) {
            participacao.estado = EstadoDesafio.SUCESSO;
        } else if (participacao.rolagens.size() < TENTATIVAS_MAXIMAS) {
            participacao.estado = EstadoDesafio.AGUARDANDO_DECISAO_DO_MESTRE;
        } else {
            participacao.estado = EstadoDesafio.FALHA;
        }
        return true;
    }

    boolean confirmarTodos() {
        boolean algumConfirmado = false;
        for (String alvoId : participacoes.keySet()) {
            algumConfirmado |= confirmar(alvoId);
        }
        return algumConfirmado;
    }

    void validarAjuda(String alvoId, Long autorId, TipoAjuda tipo) {
        Participacao participacao = participacao(alvoId);
        if (participacao.controladorId.equals(autorId)) {
            throw new IllegalArgumentException("Você não pode ajudar ou atrapalhar o próprio teste.");
        }
        if (participacao.estado == EstadoDesafio.AGUARDANDO_CONFIRMACAO) {
            if (!tipo.ehDado()) {
                throw new IllegalStateException(
                        "Depois da rolagem só é possível somar um dado bônus ou um dado de penalidade.");
            }
        } else if (!participacao.estado.aguardandoRolagem()) {
            throw new IllegalStateException("O teste de " + participacao.nomePersonagem
                    + " não aceita ajudas ou atrapalhos agora.");
        }
        if (participacao.autoresDaTentativa.contains(autorId)) {
            throw new IllegalStateException("Você já ajudou ou atrapalhou " + participacao.nomePersonagem
                    + " nesta tentativa.");
        }
    }

    void aplicarAjuda(String alvoId, Ajuda ajuda) {
        validarAjuda(alvoId, ajuda.autorId(), ajuda.tipo());
        Participacao participacao = participacao(alvoId);
        participacao.ajudas.add(ajuda);
        participacao.autoresDaTentativa.add(ajuda.autorId());
    }

    Ajuda ajuda(String alvoId, String ajudaId) {
        return participacao(alvoId).ajudas.stream()
                .filter(ajuda -> ajuda.id().equals(ajudaId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Esta ajuda não está mais no teste."));
    }

    void removerAjuda(String alvoId, String ajudaId) {
        Participacao participacao = participacao(alvoId);
        Ajuda ajuda = ajuda(alvoId, ajudaId);
        if (participacao.estado == EstadoDesafio.AGUARDANDO_CONFIRMACAO && !ajuda.tipo().ehDado()) {
            throw new IllegalStateException("A rolagem já foi feita com esta ajuda.");
        }
        participacao.ajudas.remove(ajuda);
    }

    void liberarTentativaExtra(String alvoId, ModoRolagem modo) {
        Participacao participacao = participacaoAguardandoDecisao(alvoId);
        participacao.modo = modo;
        participacao.estado = EstadoDesafio.AGUARDANDO_TENTATIVA_EXTRA;
    }

    void encerrarComFalha(String alvoId) {
        participacaoAguardandoDecisao(alvoId).estado = EstadoDesafio.FALHA;
    }

    boolean cancelar(String alvoId) {
        Participacao participacao = participacoes.get(alvoId);
        if (participacao == null || !participacao.estado.ativo()) {
            return false;
        }
        participacao.estado = EstadoDesafio.CANCELADO;
        return true;
    }

    boolean cancelarTodos() {
        boolean algumCancelado = false;
        for (String alvoId : participacoes.keySet()) {
            algumCancelado |= cancelar(alvoId);
        }
        return algumCancelado;
    }

    Set<Long> destinatariosDaMensagem(MensagemDeResultado.Destino destino, String alvoId) {
        Set<Long> destinatarios = switch (destino) {
            case JOGADOR -> {
                Participacao participacao = participacao(alvoId);
                if (participacao.estado != EstadoDesafio.SUCESSO && participacao.estado != EstadoDesafio.FALHA) {
                    throw new IllegalStateException("Só é possível enviar a mensagem depois do resultado deste jogador.");
                }
                yield Set.of(participacao.controladorId);
            }
            case VENCEDORES -> controladoresNoEstado(EstadoDesafio.SUCESSO);
            case DERROTADOS -> controladoresNoEstado(EstadoDesafio.FALHA);
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

    private Set<Long> controladoresNoEstado(EstadoDesafio estado) {
        Set<Long> controladores = new LinkedHashSet<>();
        participacoes.values().stream()
                .filter(p -> p.estado == estado)
                .forEach(p -> controladores.add(p.controladorId));
        return controladores;
    }

    private Participacao participacaoAguardandoDecisao(String alvoId) {
        Participacao participacao = participacao(alvoId);
        if (participacao.estado != EstadoDesafio.AGUARDANDO_DECISAO_DO_MESTRE) {
            throw new IllegalStateException("Só é possível decidir a tentativa extra depois de uma primeira rolagem com falha.");
        }
        return participacao;
    }
}
