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

    public static class Entrada {

        private final Long jogadorId;
        private final String nomeJogador;
        private final String nomePersonagem;
        private final int modificadorDestreza;
        private final int bonusProficiencia;
        private Iniciativa iniciativa;
        private boolean rerolagemLiberada;

        Entrada(Long jogadorId, String nomeJogador, String nomePersonagem, int modificadorDestreza,
                int bonusProficiencia) {
            this.jogadorId = jogadorId;
            this.nomeJogador = nomeJogador;
            this.nomePersonagem = nomePersonagem;
            this.modificadorDestreza = modificadorDestreza;
            this.bonusProficiencia = bonusProficiencia;
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

    private final String id;
    private final Long mestreId;
    private final Instant criadoEm;
    private final Map<Long, Entrada> entradas = new LinkedHashMap<>();
    private Fase fase = Fase.INICIATIVA;
    private List<Long> ordem = List.of();
    private int indiceDoTurno;
    private int rodada = 1;

    public Combate(String id, Long mestreId, Instant criadoEm) {
        this.id = id;
        this.mestreId = mestreId;
        this.criadoEm = criadoEm;
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

    public boolean vazio() {
        return entradas.isEmpty();
    }

    public boolean participa(Long jogadorId) {
        return entradas.containsKey(jogadorId);
    }

    public Long jogadorDoTurno() {
        return emAndamento() ? ordem.get(indiceDoTurno) : null;
    }

    public List<Entrada> entradasEmOrdem() {
        if (!emAndamento()) {
            return List.copyOf(entradas.values());
        }
        return ordem.stream().map(entradas::get).toList();
    }

    public Entrada entrada(Long jogadorId) {
        Entrada entrada = entradas.get(jogadorId);
        if (entrada == null) {
            throw new IllegalStateException("Este jogador não participa do combate.");
        }
        return entrada;
    }

    public boolean podeRolarLivremente(Long userId) {
        return emAndamento() && ordem.get(indiceDoTurno).equals(userId);
    }

    void adicionar(Long jogadorId, String nomeJogador, String nomePersonagem, int modificadorDestreza,
                   int bonusProficiencia) {
        entradas.put(jogadorId, new Entrada(jogadorId, nomeJogador, nomePersonagem, modificadorDestreza,
                bonusProficiencia));
    }

    void exigirAguardandoRolagem(Long jogadorId) {
        if (!entrada(jogadorId).aguardandoRolagem()) {
            throw new IllegalStateException(
                    "Você já rolou a iniciativa. Peça ao mestre para liberar uma nova rolagem.");
        }
    }

    void registrarIniciativa(Long jogadorId, Iniciativa iniciativa) {
        exigirAguardandoRolagem(jogadorId);
        Entrada entrada = entrada(jogadorId);
        entrada.iniciativa = iniciativa;
        entrada.rerolagemLiberada = false;
        reordenar();
    }

    void liberarRerolagem(List<Long> jogadorIds) {
        if (!emAndamento()) {
            throw new IllegalStateException("A rolagem só pode ser liberada depois que todos rolarem a iniciativa.");
        }
        List<Entrada> escolhidas = jogadorIds.stream().map(this::entrada).toList();
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

    void remover(Long jogadorId) {
        if (entradas.remove(jogadorId) == null || entradas.isEmpty()) {
            return;
        }
        if (!emAndamento()) {
            reordenar();
            return;
        }
        int posicao = ordem.indexOf(jogadorId);
        List<Long> restantes = new ArrayList<>(ordem);
        restantes.remove(jogadorId);
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
        Long atual = jogadorDoTurno();
        ordem = entradas.values().stream()
                .sorted(Comparator.comparingInt((Entrada entrada) -> entrada.iniciativa.total()).reversed()
                        .thenComparing(Comparator.comparingInt((Entrada entrada) -> entrada.modificadorDestreza)
                                .reversed()))
                .map(Entrada::getJogadorId)
                .toList();
        fase = Fase.EM_ANDAMENTO;
        indiceDoTurno = atual == null ? 0 : ordem.indexOf(atual);
    }
}
