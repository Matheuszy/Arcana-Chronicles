package com.arcana.backend.presence;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;

@Component
public class CanalDoUsuarioHandler extends TextWebSocketHandler {

    public static final int LIMITE_CONEXOES_POR_USUARIO = 10;

    private final CanalDoUsuario canal;
    private final PresencaService presencaService;

    public CanalDoUsuarioHandler(CanalDoUsuario canal, PresencaService presencaService) {
        this.canal = canal;
        this.presencaService = presencaService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws IOException {
        Long usuarioId = usuarioDaSessao(session);
        if (usuarioId == null) {
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }
        if (canal.registrar(usuarioId, session) > LIMITE_CONEXOES_POR_USUARIO) {
            canal.remover(usuarioId, session.getId());
            session.close(CloseStatus.POLICY_VIOLATION.withReason("Conexões demais abertas."));
            return;
        }
        presencaService.aoConectar(usuarioId);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long usuarioId = usuarioDaSessao(session);
        if (usuarioId == null) return;
        canal.remover(usuarioId, session.getId());
        presencaService.aoDesconectar(usuarioId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
    }

    private Long usuarioDaSessao(WebSocketSession session) {
        Object valor = session.getAttributes().get(AutenticacaoCanalDoUsuarioInterceptor.ATRIBUTO_USUARIO_ID);
        return valor instanceof Long id ? id : null;
    }
}
