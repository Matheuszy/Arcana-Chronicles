package com.arcana.backend.user.service;

import com.arcana.backend.user.model.CodigoAmigo;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class GeradorCodigoAmigo {

    private final SecureRandom aleatorio = new SecureRandom();

    public String gerar() {
        StringBuilder codigo = new StringBuilder(CodigoAmigo.TAMANHO);
        for (int posicao = 0; posicao < CodigoAmigo.TAMANHO; posicao++) {
            codigo.append(CodigoAmigo.ALFABETO.charAt(aleatorio.nextInt(CodigoAmigo.ALFABETO.length())));
        }
        return codigo.toString();
    }
}
