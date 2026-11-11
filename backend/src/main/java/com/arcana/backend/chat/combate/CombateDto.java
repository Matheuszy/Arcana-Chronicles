package com.arcana.backend.chat.combate;

import java.util.List;

public record CombateDto(
        String id,
        String nome,
        Combate.Fase fase,
        int rodada,
        String turnoDe,
        Long controladorDoTurno,
        boolean turnoOculto,
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
            boolean aguardandoFimDoTurno,
            boolean oculto,
            boolean reacaoUsada,
            Combate.Reacao reacaoAberta,
            Iniciativa iniciativa
    ) {

        static EntradaDto de(Combate.Entrada entrada) {
            return new EntradaDto(entrada.getId(), entrada.getTipo(), entrada.getControladorId(),
                    entrada.getPersonagemId(), entrada.getGrupo(), entrada.getNomeJogador(),
                    entrada.getNomePersonagem(), entrada.aguardandoRolagem(), entrada.isRerolagemLiberada(),
                    entrada.isAguardandoFimDoTurno(), entrada.isOculto(), entrada.isReacaoUsada(),
                    entrada.getReacaoAberta(), entrada.getIniciativa());
        }
    }

    public static CombateDto de(Combate combate) {
        return new CombateDto(
                combate.getId(),
                combate.getNome(),
                combate.getFase(),
                combate.getRodada(),
                combate.idDoTurno(),
                combate.controladorDoTurno(),
                combate.turnoOculto(),
                combate.entradasEmOrdem().stream().map(EntradaDto::de).toList());
    }

    public static CombateDto paraJogador(Combate combate) {
        boolean turnoOculto = combate.turnoOculto();
        return new CombateDto(
                combate.getId(),
                combate.getNome(),
                combate.getFase(),
                combate.getRodada(),
                turnoOculto ? null : combate.idDoTurno(),
                combate.controladorDoTurno(),
                turnoOculto,
                combate.entradasEmOrdem().stream()
                        .filter(entrada -> !entrada.isOculto())
                        .map(EntradaDto::de)
                        .toList());
    }
}
