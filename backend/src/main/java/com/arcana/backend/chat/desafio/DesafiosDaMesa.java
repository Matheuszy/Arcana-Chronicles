package com.arcana.backend.chat.desafio;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

public class DesafiosDaMesa {

    public static final int LIMITE_CONCLUIDOS = 20;

    private final Map<String, Desafio> desafios = new LinkedHashMap<>();

    public synchronized DesafioDto adicionar(Desafio desafio) {
        for (Desafio.Participacao participacao : desafio.participacoes()) {
            if (alvoEmDesafioAtivo(participacao.getId())) {
                String nome = participacao.getTipo() == Desafio.TipoAlvo.JOGADOR
                        ? participacao.getNomeJogador()
                        : participacao.getNomePersonagem();
                throw new IllegalStateException(nome + " já tem um desafio em andamento.");
            }
        }
        desafios.put(desafio.getId(), desafio);
        descartarConcluidosExcedentes();
        return DesafioDto.paraMestre(desafio);
    }

    public synchronized boolean alvoEmDesafioAtivo(String alvoId) {
        return desafios.values().stream().anyMatch(d -> d.alvoAtivo(alvoId));
    }

    public synchronized boolean jogadorEmDesafioAtivo(Long jogadorId) {
        return desafios.values().stream().anyMatch(d -> d.jogadorAtivo(jogadorId));
    }

    public synchronized Optional<DesafioDto> ativoDoJogador(Long jogadorId) {
        return desafios.values().stream()
                .filter(d -> d.jogadorAtivo(jogadorId))
                .findFirst()
                .map(d -> DesafioDto.paraUsuario(d, jogadorId));
    }

    public synchronized DesafioDto alterar(String desafioId, Long visualizadorId, Consumer<Desafio> alteracao) {
        Desafio desafio = buscar(desafioId);
        alteracao.accept(desafio);
        return DesafioDto.paraUsuario(desafio, visualizadorId);
    }

    public synchronized DesafioDto visaoPara(String desafioId, Long userId) {
        Desafio desafio = buscar(desafioId);
        if (!desafio.envolve(userId)) {
            throw new IllegalArgumentException("Desafio não encontrado.");
        }
        return DesafioDto.paraUsuario(desafio, userId);
    }

    public synchronized Set<Long> envolvidos(String desafioId) {
        return buscar(desafioId).destinatarios();
    }

    public synchronized List<DesafioDto> visiveisPara(Long userId) {
        return desafios.values().stream()
                .filter(d -> d.envolve(userId))
                .map(d -> DesafioDto.paraUsuario(d, userId))
                .toList();
    }

    public synchronized List<DesafioDto> cancelarDoJogador(Long jogadorId) {
        List<DesafioDto> alterados = new ArrayList<>();
        for (Desafio desafio : desafios.values()) {
            boolean cancelou = desafio.cancelar(Desafio.idDoJogador(jogadorId));
            if (desafio.getCriadorId().equals(jogadorId)) {
                cancelou |= desafio.cancelarTodos();
            }
            if (cancelou) {
                alterados.add(DesafioDto.paraMestre(desafio));
            }
        }
        return alterados;
    }

    public synchronized List<DesafioDto> cancelarDoCombate() {
        List<DesafioDto> alterados = new ArrayList<>();
        for (Desafio desafio : desafios.values()) {
            if (desafio.isDoCombate() && desafio.cancelarTodos()) {
                alterados.add(DesafioDto.paraMestre(desafio));
            }
        }
        return alterados;
    }

    public synchronized List<DesafioDto> cancelarTodos() {
        List<DesafioDto> alterados = new ArrayList<>();
        for (Desafio desafio : desafios.values()) {
            if (desafio.cancelarTodos()) {
                alterados.add(DesafioDto.paraMestre(desafio));
            }
        }
        return alterados;
    }

    private Desafio buscar(String desafioId) {
        Desafio desafio = desafios.get(desafioId);
        if (desafio == null) {
            throw new IllegalArgumentException("Desafio não encontrado.");
        }
        return desafio;
    }

    private void descartarConcluidosExcedentes() {
        List<String> concluidos = desafios.values().stream()
                .filter(d -> !d.ativo())
                .map(Desafio::getId)
                .toList();
        concluidos.stream()
                .limit(Math.max(0, concluidos.size() - LIMITE_CONCLUIDOS))
                .forEach(desafios::remove);
    }
}
