package com.arcana.backend.chat.combate;

import java.util.List;

public record ResumoCombateDto(
        String id,
        String nome,
        Combate.Fase fase,
        int rodada,
        List<String> jogadores
) {

    public static ResumoCombateDto de(Combate combate) {
        return new ResumoCombateDto(combate.getId(), combate.getNome(), combate.getFase(), combate.getRodada(),
                combate.nomesDosJogadores());
    }
}
