package com.arcana.backend.chat.sala;

import java.util.List;

public record StatusDto(List<Atual> atuais, List<Registro> historico) {

    public record Atual(Long personagemId, String nome, boolean npc, String texto, String atualizadoEm) {}

    public record Registro(String id, Long personagemId, String nome, boolean npc, String acao, String texto,
                           String criadoEm) {}
}
