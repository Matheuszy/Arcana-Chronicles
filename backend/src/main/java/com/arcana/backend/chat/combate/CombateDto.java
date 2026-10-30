package com.arcana.backend.chat.combate;

import java.util.List;

public record CombateDto(
        String id,
        Combate.Fase fase,
        int rodada,
        String turnoDe,
        Long controladorDoTurno,
        List<EntradaDto> participantes
) {

    public record EntradaDto(
            String id,
            Combate.TipoEntrada tipo,
            Long controladorId,
            Long personagemId,
            String grupo,
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
                combate.idDoTurno(),
                combate.controladorDoTurno(),
                combate.entradasEmOrdem().stream()
                        .map(entrada -> new EntradaDto(entrada.getId(), entrada.getTipo(),
                                entrada.getControladorId(), entrada.getPersonagemId(), entrada.getGrupo(),
                                entrada.getNomeJogador(), entrada.getNomePersonagem(), entrada.aguardandoRolagem(),
                                entrada.isRerolagemLiberada(), entrada.getIniciativa()))
                        .toList());
    }
}
