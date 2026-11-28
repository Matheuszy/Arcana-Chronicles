package com.arcana.backend.character.service;

import java.text.Normalizer;
import java.util.Locale;

final class TermoDeBusca {

    private TermoDeBusca() {}

    static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        String semAcentos = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return semAcentos.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
