package com.arcana.backend.chat.sala;

import java.util.HashMap;
import java.util.Map;

public class NotasDaMesa {

    public static final int LIMITE_CARACTERES = 2_000;

    private final Map<Long, String> notas = new HashMap<>();

    public synchronized String salvar(Long userId, String texto) {
        String conteudo = texto == null ? "" : texto;
        if (conteudo.length() > LIMITE_CARACTERES) {
            throw new IllegalArgumentException(
                    "A anotação passa do limite de " + LIMITE_CARACTERES + " caracteres.");
        }
        if (conteudo.isBlank()) {
            notas.remove(userId);
            return "";
        }
        notas.put(userId, conteudo);
        return conteudo;
    }

    public synchronized String de(Long userId) {
        return notas.getOrDefault(userId, "");
    }
}
