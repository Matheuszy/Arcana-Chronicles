package com.arcana.backend.presence;

import com.arcana.backend.presence.model.MensagemDoCanal;

import java.util.Collection;
import java.util.List;

public record NotificacaoDeUsuarioEvent(Collection<Long> usuarioIds, MensagemDoCanal mensagem) {

    public static NotificacaoDeUsuarioEvent para(MensagemDoCanal mensagem, Long... usuarioIds) {
        return new NotificacaoDeUsuarioEvent(List.of(usuarioIds), mensagem);
    }
}
