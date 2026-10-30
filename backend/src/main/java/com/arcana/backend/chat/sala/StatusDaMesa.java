package com.arcana.backend.chat.sala;

import com.arcana.backend.table.AlvoDeStatus;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class StatusDaMesa {

    public static final int LIMITE_CARACTERES = 200;
    public static final int LIMITE_FICHAS = 50;
    public static final int LIMITE_HISTORICO = 100;
    public static final String DEFINIDO = "DEFINIDO";
    public static final String REMOVIDO = "REMOVIDO";

    private record Entrada(Long personagemId, String nome, boolean npc, Long donoId, String texto,
                           String atualizadoEm) {}

    private final Map<Long, Entrada> atuais = new LinkedHashMap<>();
    private final Deque<StatusDto.Registro> historico = new ArrayDeque<>();

    public synchronized StatusDto.Registro definir(AlvoDeStatus alvo, String texto, Instant agora) {
        String conteudo = texto == null ? "" : texto.trim();
        if (conteudo.isEmpty()) {
            throw new IllegalArgumentException("Escreva o status antes de aplicar.");
        }
        if (conteudo.length() > LIMITE_CARACTERES) {
            throw new IllegalArgumentException(
                    "O status passa do limite de " + LIMITE_CARACTERES + " caracteres.");
        }
        if (!atuais.containsKey(alvo.personagemId()) && atuais.size() >= LIMITE_FICHAS) {
            throw new IllegalStateException("A mesa já tem " + LIMITE_FICHAS + " fichas com status.");
        }

        atuais.put(alvo.personagemId(), new Entrada(alvo.personagemId(), alvo.nome(), alvo.doMestre(),
                alvo.donoId(), conteudo, agora.toString()));
        return registrar(alvo.personagemId(), alvo.nome(), alvo.doMestre(), DEFINIDO, conteudo, agora);
    }

    public synchronized StatusDto.Registro remover(Long personagemId, Instant agora) {
        Entrada removida = atuais.remove(personagemId);
        if (removida == null) {
            throw new IllegalArgumentException("Esta ficha não tem status.");
        }
        return registrar(removida.personagemId(), removida.nome(), removida.npc(), REMOVIDO, removida.texto(), agora);
    }

    public synchronized List<StatusDto.Registro> removerDoDono(Long donoId, Instant agora) {
        List<Long> fichas = atuais.values().stream()
                .filter(entrada -> entrada.donoId().equals(donoId))
                .map(Entrada::personagemId)
                .toList();
        return fichas.stream().map(ficha -> remover(ficha, agora)).toList();
    }

    public synchronized StatusDto visaoPara(boolean mestre) {
        List<StatusDto.Atual> visiveis = atuais.values().stream()
                .filter(entrada -> mestre || !entrada.npc())
                .map(entrada -> new StatusDto.Atual(entrada.personagemId(), entrada.nome(), entrada.npc(),
                        entrada.texto(), entrada.atualizadoEm()))
                .toList();
        List<StatusDto.Registro> registros = historico.stream()
                .filter(registro -> mestre || !registro.npc())
                .toList();
        return new StatusDto(visiveis, registros);
    }

    private StatusDto.Registro registrar(Long personagemId, String nome, boolean npc, String acao, String texto,
                                         Instant agora) {
        StatusDto.Registro registro = new StatusDto.Registro(UUID.randomUUID().toString(), personagemId, nome, npc,
                acao, texto, agora.toString());
        historico.addLast(registro);
        while (historico.size() > LIMITE_HISTORICO) {
            historico.removeFirst();
        }
        return registro;
    }
}
