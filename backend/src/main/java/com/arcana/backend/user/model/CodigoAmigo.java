package com.arcana.backend.user.model;

import java.util.Locale;
import java.util.Optional;

public final class CodigoAmigo {

    public static final String ALFABETO = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    public static final int TAMANHO = 8;
    private static final String PREFIXO = "ARC";

    private CodigoAmigo() {}

    public static String formatar(String codigo) {
        if (codigo == null) {
            return null;
        }
        int metade = TAMANHO / 2;
        return PREFIXO + "-" + codigo.substring(0, metade) + "-" + codigo.substring(metade);
    }

    public static Optional<String> normalizar(String entrada) {
        if (entrada == null) {
            return Optional.empty();
        }
        String codigo = entrada.replaceAll("[\\s-]", "").toUpperCase(Locale.ROOT);
        if (codigo.length() == PREFIXO.length() + TAMANHO && codigo.startsWith(PREFIXO)) {
            codigo = codigo.substring(PREFIXO.length());
        }
        boolean valido = codigo.length() == TAMANHO
                && codigo.chars().allMatch(caractere -> ALFABETO.indexOf(caractere) >= 0);
        return valido ? Optional.of(codigo) : Optional.empty();
    }
}
