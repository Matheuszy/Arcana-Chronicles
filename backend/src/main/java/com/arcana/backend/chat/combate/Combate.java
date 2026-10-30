package com.arcana.backend.chat.combate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Combate {

    public enum Fase {
        INICIATIVA,
        EM_ANDAMENTO
    }

    public enum TipoEntrada {
        JOGADOR,
        MONSTRO
    }

    public static class Entrada {

        private final String id;
        private final TipoEntrada tipo;
        private final Long controladorId;
        private final Long personagemId;
        private final String grupo;
        private final String nomeJogador;
        private final String nomePersonagem;
        private final int modificadorDestreza;
        private final int bonusProficiencia;
        private Iniciativa iniciativa;
        private boolean rerolagemLiberada;

        Entrada(String id, TipoEntrada tipo, Long controladorId, Long personagemId, String grupo,
                String nomeJogador, String nomePersonagem, int modificadorDestreza, int bonusProficiencia) {
            this.id = id;
            this.tipo = tipo;
            this.controladorId = controladorId;
            this.personagemId = personagemId;
            this.grupo = grupo;
            this.nomeJogador = nomeJogador;
            this.nomePersonagem = nomePersonagem;
            this.modificadorDestreza = modificadorDestreza;
            this.bonusProficiencia = bonusProficiencia;
        }

        public String getId() {
            return id;
        }

        public TipoEntrada getTipo() {
            return tipo;
        }

        public Long getControladorId() {
            return controladorId;
        }

        public Long getPersonagemId() {
            return personagemId;
        }

        public String getGrupo() {
            return grupo;
        }

        public String getNomeJogador() {
            return nomeJogador;
        }

        public String getNomePersonagem() {
            return nomePersonagem;
        }

        public int getModificadorDestreza() {
            return modificadorDestreza;
        }

        public int getBonusProficiencia() {
            return bonusProficiencia;
        }

        public Iniciativa getIniciativa() {
            return iniciativa;
        }

        public boolean isRerolagemLiberada() {
            return rerolagemLiberada;
        }

        public boolean aguardandoRolagem() {
            return iniciativa == null || rerolagemLiberada;
        }
    }

    private static final String PREFIXO_MONSTRO = "monstro-";

    private final String id;
    private final Long mestreId;
    private final Instant criadoEm;
    private final Map<String, Entrada> entradas = new LinkedHashMap<>();
    private Fase fase = Fase.INICIATIVA;
    private List<String> ordem = List.of();
    private int indiceDoTurno;
    private int rodada = 1;
    private int monstrosAdicionados;

    public Combate(String id, Long mestreId, Instant criadoEm) {
        this.id = id;
        this.mestreId = mestreId;
        this.criadoEm = criadoEm;
    }

    public static String idDoJogador(Long jogadorId) {
        return String.valueOf(jogadorId);
    }

    public String getId() {
        return id;
    }

    public Long getMestreId() {
        return mestreId;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Fase getFase() {
        return fase;
    }

    public int getRodada() {
        return rodada;
    }

    public boolean emAndamento() {
        return fase == Fase.EM_ANDAMENTO;
    }

    public boolean semJogadores() {
        return entradas.values().stream().noneMatch(entrada -> entrada.tipo == TipoEntrada.JOGADOR);
    }

    public String idDoTurno() {
        return emAndamento() ? ordem.get(indiceDoTurno) : null;
    }

    public Long controladorDoTurno() {
        return emAndamento() ? entradas.get(ordem.get(indiceDoTurno)).controladorId : null;
    }

    public List<Entrada> entradasEmOrdem() {
        if (!emAndamento()) {
            return List.copyOf(entradas.values());
        }
        return ordem.stream().map(entradas::get).toList();
    }

    public Entrada entrada(String entradaId) {
        Entrada entrada = entradas.get(entradaId);
        if (entrada == null) {
            throw new IllegalStateException("Este jogador não participa do combate.");
        }
        return entrada;
    }

    public boolean podeRolarLivremente(Long userId) {
        return emAndamento() && userId != null && userId.equals(controladorDoTurno());
    }

    void adicionarJogador(Long jogadorId, String nomeJogador, String nomePersonagem, int modificadorDestreza,
                          int bonusProficiencia) {
        String entradaId = idDoJogador(jogadorId);
        entradas.put(entradaId, new Entrada(entradaId, TipoEntrada.JOGADOR, jogadorId, null, null, nomeJogador,
                nomePersonagem, modificadorDestreza, bonusProficiencia));
    }

    void adicionarMonstro(Long personagemId, String grupo, String nomeMonstro, String nomeMestre,
                          int modificadorDestreza, int bonusProficiencia) {
        String entradaId = PREFIXO_MONSTRO + (++monstrosAdicionados);
        entradas.put(entradaId, new Entrada(entradaId, TipoEntrada.MONSTRO, mestreId, personagemId, grupo,
                nomeMestre, nomeMonstro, modificadorDestreza, bonusProficiencia));
    }

    void exigirAguardandoRolagem(String entradaId) {
        if (!entrada(entradaId).aguardandoRolagem()) {
            throw new IllegalStateException(
                    "Você já rolou a iniciativa. Peça ao mestre para liberar uma nova rolagem.");
        }
    }

    void registrarIniciativa(String entradaId, Iniciativa iniciativa) {
        exigirAguardandoRolagem(entradaId);
        Entrada entrada = entrada(entradaId);
        entrada.iniciativa = iniciativa;
        entrada.rerolagemLiberada = false;
        reordenar();
    }

    void liberarRerolagem(List<String> entradaIds) {
        if (!emAndamento()) {
            throw new IllegalStateException("A rolagem só pode ser liberada depois que todos rolarem a iniciativa.");
        }
        List<Entrada> escolhidas = entradaIds.stream().map(this::entrada).toList();
        escolhidas.forEach(entrada -> entrada.rerolagemLiberada = true);
    }

    void passarTurno() {
        if (!emAndamento()) {
            throw new IllegalStateException("O combate só começa depois que todos rolarem a iniciativa.");
        }
        indiceDoTurno++;
        if (indiceDoTurno >= ordem.size()) {
            indiceDoTurno = 0;
            rodada++;
        }
    }

    void removerJogador(Long jogadorId) {
        remover(idDoJogador(jogadorId));
    }

    void remover(String entradaId) {
        if (entradas.remove(entradaId) == null || semJogadores()) {
            return;
        }
        if (!emAndamento()) {
            reordenar();
            return;
        }
        int posicao = ordem.indexOf(entradaId);
        List<String> restantes = new ArrayList<>(ordem);
        restantes.remove(entradaId);
        ordem = List.copyOf(restantes);
        if (posicao < indiceDoTurno) {
            indiceDoTurno--;
        } else if (indiceDoTurno >= ordem.size()) {
            indiceDoTurno = 0;
            rodada++;
        }
    }

    private void reordenar() {
        if (entradas.values().stream().anyMatch(entrada -> entrada.iniciativa == null)) {
            return;
        }
        String atual = idDoTurno();
        ordem = entradas.values().stream()
                .sorted(Comparator.comparingInt((Entrada entrada) -> entrada.iniciativa.total()).reversed()
                        .thenComparing(Comparator.comparingInt((Entrada entrada) -> entrada.modificadorDestreza)
                                .reversed()))
                .map(Entrada::getId)
                .toList();
        fase = Fase.EM_ANDAMENTO;
        indiceDoTurno = atual == null ? 0 : ordem.indexOf(atual);
    }
}
