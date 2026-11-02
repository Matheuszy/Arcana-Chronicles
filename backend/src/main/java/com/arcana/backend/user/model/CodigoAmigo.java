package com.arcana.backend.user.model;

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
}
