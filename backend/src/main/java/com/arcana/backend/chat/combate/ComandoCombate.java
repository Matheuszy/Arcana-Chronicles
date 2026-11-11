package com.arcana.backend.chat.combate;

import com.arcana.backend.chat.dice.ModoRolagem;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ComandoCombate(
        String combateId,
        String nome,
        List<Long> jogadores,
        List<GrupoDeMonstros> monstros,
        List<String> entradas,
        ModoRolagem modo,
        String entradaId,
        Combate.TipoReacao tipoReacao,
        String descricao
) {

    public static ComandoCombate vazio() {
        return new ComandoCombate(null, null, null, null, null, null, null, null, null);
    }

    public record GrupoDeMonstros(Long personagemId, Integer quantidade, String nome, Boolean oculto) {

        public GrupoDeMonstros(Long personagemId, Integer quantidade, String nome) {
            this(personagemId, quantidade, nome, false);
        }

        public boolean ehOculto() {
            return Boolean.TRUE.equals(oculto);
        }
    }
}
