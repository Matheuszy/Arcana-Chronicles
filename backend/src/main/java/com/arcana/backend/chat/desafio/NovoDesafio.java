package com.arcana.backend.chat.desafio;

import com.arcana.backend.chat.dice.ModoRolagem;

import java.util.List;

public record NovoDesafio(Integer cd, String descricao, List<Convocacao> jogadores) {

    public record Convocacao(Long jogadorId, ModoRolagem modo, String alvoId) {

        public Convocacao(Long jogadorId, ModoRolagem modo) {
            this(jogadorId, modo, null);
        }

        public String alvo() {
            if (alvoId != null && !alvoId.isBlank()) {
                return alvoId.trim();
            }
            return jogadorId == null ? null : Desafio.idDoJogador(jogadorId);
        }
    }
}
