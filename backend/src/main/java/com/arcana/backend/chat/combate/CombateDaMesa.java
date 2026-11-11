package com.arcana.backend.chat.combate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

public class CombateDaMesa {

    private final Map<String, Combate> combates = new LinkedHashMap<>();
    private int combatesCriados;

    public synchronized String proximoNomePadrao() {
        return "Combate " + (combatesCriados + 1);
    }

    public synchronized CombateDto iniciar(Combate novo) {
        for (Combate.Entrada entrada : novo.entradasEmOrdem()) {
            if (entrada.getTipo() == Combate.TipoEntrada.JOGADOR && doJogador(entrada.getControladorId()).isPresent()) {
                throw new IllegalStateException(entrada.getNomePersonagem() + " já está em outro combate.");
            }
        }
        combatesCriados++;
        combates.put(novo.getId(), novo);
        return CombateDto.de(novo);
    }

    public synchronized CombateDto alterar(String combateId, Consumer<Combate> alteracao) {
        Combate combate = escolher(combateId);
        alteracao.accept(combate);
        return CombateDto.de(combate);
    }

    public synchronized Combate adicionar(String combateId, Map<Long, String> jogadoresComPersonagem,
                                          Consumer<Combate> adicao) {
        Combate combate = escolher(combateId);
        jogadoresComPersonagem.forEach((jogadorId, personagem) -> doJogador(jogadorId).ifPresent(atual -> {
            String onde = atual == combate ? "neste combate" : "em outro combate";
            throw new IllegalStateException(personagem + " já está " + onde + ".");
        }));
        adicao.accept(combate);
        return combate;
    }

    public synchronized CombateDto alterarDoJogador(Long jogadorId, Consumer<Combate> alteracao) {
        Combate combate = doJogador(jogadorId)
                .orElseThrow(() -> new IllegalStateException("Este jogador não participa do combate."));
        alteracao.accept(combate);
        return CombateDto.de(combate);
    }

    public synchronized List<CombateDto> estados() {
        return combates.values().stream().map(CombateDto::de).toList();
    }

    public synchronized Optional<CombateDto> estadoDoJogador(Long jogadorId) {
        return doJogador(jogadorId).map(CombateDto::paraJogador);
    }

    public synchronized <T> T consultar(String combateId, Function<Combate, T> consulta) {
        return consulta.apply(escolher(combateId));
    }

    public synchronized <T> T consultarDoJogador(Long jogadorId, Function<Combate, T> consulta) {
        Combate combate = doJogador(jogadorId)
                .orElseThrow(() -> new IllegalStateException("Este jogador não participa do combate."));
        return consulta.apply(combate);
    }

    public synchronized void usarReacaoParaAjudar(Long jogadorId) {
        doJogador(jogadorId).ifPresent(combate -> combate.usarReacaoParaAjudar(Combate.idDoJogador(jogadorId)));
    }

    public synchronized Optional<ReacaoDeclarada> reacaoAbertaDe(Long userId) {
        for (Combate combate : combates.values()) {
            Optional<Combate.Entrada> entrada = combate.reacaoAbertaDe(userId);
            if (entrada.isPresent()) {
                return Optional.of(new ReacaoDeclarada(combate.getId(), entrada.get().getNomePersonagem(),
                        entrada.get().getReacaoAberta()));
            }
        }
        return Optional.empty();
    }

    public synchronized CombatesDaMesaDto visaoDo(Long userId, boolean ehMestre) {
        List<CombateDto> completos = new ArrayList<>();
        List<ResumoCombateDto> resumos = new ArrayList<>();
        for (Combate combate : combates.values()) {
            if (ehMestre) {
                completos.add(CombateDto.de(combate));
            } else if (combate.temJogador(userId)) {
                completos.add(CombateDto.paraJogador(combate));
            } else {
                resumos.add(ResumoCombateDto.de(combate));
            }
        }
        return new CombatesDaMesaDto(completos, resumos);
    }

    public synchronized Set<String> ids() {
        return Set.copyOf(combates.keySet());
    }

    public synchronized boolean podeRolarLivremente(Long userId) {
        List<Combate> envolvidos = combates.values().stream()
                .filter(combate -> combate.controlaAlgumaEntrada(userId))
                .toList();
        return envolvidos.isEmpty() || envolvidos.stream().anyMatch(combate -> combate.podeRolarLivremente(userId));
    }

    public synchronized void removerJogador(Long jogadorId) {
        doJogador(jogadorId).ifPresent(combate -> {
            combate.removerJogador(jogadorId);
            if (combate.semJogadores()) {
                combates.remove(combate.getId());
            }
        });
    }

    public synchronized void encerrar(String combateId) {
        combates.remove(escolher(combateId).getId());
    }

    private Optional<Combate> doJogador(Long jogadorId) {
        return combates.values().stream().filter(combate -> combate.temJogador(jogadorId)).findFirst();
    }

    private Combate escolher(String combateId) {
        if (combates.isEmpty()) {
            throw new IllegalStateException("Não há combate em andamento.");
        }
        if (combateId == null || combateId.isBlank()) {
            if (combates.size() > 1) {
                throw new IllegalArgumentException("Escolha o combate.");
            }
            return combates.values().iterator().next();
        }
        Combate combate = combates.get(combateId);
        if (combate == null) {
            throw new IllegalStateException("Este combate não está mais em andamento.");
        }
        return combate;
    }
}
