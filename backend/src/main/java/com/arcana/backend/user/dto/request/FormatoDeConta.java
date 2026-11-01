package com.arcana.backend.user.dto.request;

import java.util.Locale;

public final class FormatoDeConta {

    public static final String EMAIL_VALIDO =
            "^[A-Za-z0-9._%+-]+@[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?)*\\.[A-Za-z]{2,}$";

    public static final String MENSAGEM_EMAIL_INVALIDO = "Email inválido. Use o formato nome@dominio.com";

    private FormatoDeConta() {}

    public static String normalizarEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    public static String normalizarUsername(String username) {
        return username == null ? null : username.trim();
    }
}
