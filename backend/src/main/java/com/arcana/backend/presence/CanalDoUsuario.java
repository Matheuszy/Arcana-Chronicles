package com.arcana.backend.presence;

import com.arcana.backend.presence.model.MensagemDoCanal;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class CanalDoUsuario {

    private static final int LIMITE_TEMPO_ENVIO_MS = 10_000;
    private static final int LIMITE_BUFFER_ENVIO_BYTES = 512 * 1024;

    private final ObjectMapper objectMapper;
    private final Map<Long, Map<String, WebSocketSession>> sessoesPorUsuario = new ConcurrentHashMap<>();

    public CanalDoUsuario(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public int registrar(Long usuarioId, WebSocketSession sessao) {
        WebSocketSession sessaoSegura = new ConcurrentWebSocketSessionDecorator(
                sessao, LIMITE_TEMPO_ENVIO_MS, LIMITE_BUFFER_ENVIO_BYTES);
        int[] total = new int[1];
        sessoesPorUsuario.compute(usuarioId, (id, sessoes) -> {
            Map<String, WebSocketSession> atuais = sessoes != null ? sessoes : new ConcurrentHashMap<>();
            atuais.put(sessao.getId(), sessaoSegura);
            total[0] = atuais.size();
            return atuais;
        });
        return total[0];
    }

    public void remover(Long usuarioId, String sessionId) {
        sessoesPorUsuario.computeIfPresent(usuarioId, (id, sessoes) -> {
            sessoes.remove(sessionId);
            return sessoes.isEmpty() ? null : sessoes;
        });
    }

    public boolean conectado(Long usuarioId) {
        return sessoesPorUsuario.containsKey(usuarioId);
    }

    public void enviar(Collection<Long> usuarioIds, MensagemDoCanal mensagem) {
        String conteudo;
        try {
            conteudo = objectMapper.writeValueAsString(mensagem);
        } catch (JsonProcessingException e) {
            return;
        }
        for (Long usuarioId : usuarioIds) {
            for (WebSocketSession sessao : sessoesDe(usuarioId)) {
                if (!sessao.isOpen()) continue;
                try {
                    sessao.sendMessage(new TextMessage(conteudo));
                } catch (IOException | IllegalStateException ignored) {}
            }
        }
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void aoNotificar(NotificacaoDeUsuarioEvent evento) {
        enviar(evento.usuarioIds(), evento.mensagem());
    }

    private List<WebSocketSession> sessoesDe(Long usuarioId) {
        Map<String, WebSocketSession> sessoes = sessoesPorUsuario.get(usuarioId);
        return sessoes == null ? List.of() : List.copyOf(sessoes.values());
    }
}
