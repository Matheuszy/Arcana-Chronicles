package com.arcana.backend.chat.combate;

import java.util.List;

public record CombateDto(
        String id,
        Combate.Fase fase,
        int rodada,
        Long turnoDe,
        List<EntradaDto> participantes
) {

    public record EntradaDto(
            Long jogadorId,
            String nomeJogador,
            String nomePersonagem,
            boolean aguardandoRolagem,
            boolean rerolagemLiberada,
            Iniciativa iniciativa
    ) {}

    public static CombateDto de(Combate combate) {
        return new CombateDto(
                combate.getId(),
                combate.getFase(),
                combate.getRodada(),
                combate.jogadorDoTurno(),
                combate.entradasEmOrdem().stream()
                        .map(entrada -> new EntradaDto(entrada.getJogadorId(), entrada.getNomeJogador(),
                                entrada.getNomePersonagem(), entrada.aguardandoRolagem(),
                                entrada.isRerolagemLiberada(), entrada.getIniciativa()))
                        .toList());
    }
}
