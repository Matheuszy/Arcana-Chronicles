package com.arcana.backend.chat;

import com.arcana.backend.chat.dice.RolagemDadosService;
import com.arcana.backend.chat.model.ChatMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Handler WebSocket para o chat da mesa.
 * URL: ws://localhost:8080/ws/mesas/{tableId}
 *
 * Cada mesa tem seu próprio conjunto de sessões ativas.
 * Quando uma mensagem chega, é rebroadcastada para todos os participantes da mesma mesa.
 * Mensagens com @menção ficam marcadas com mentionedNpcId — a integração com IA entrará aqui.
 */
@Component
public class MesaChatHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final RolagemDadosService rolagemDadosService;

    /** tableId → conjunto de sessões ativas nessa mesa */
    private final Map<String, Set<WebSocketSession>> rooms = new ConcurrentHashMap<>();
    private final Map<String, WebSocketSession> sessoesComEnvioSerializado = new ConcurrentHashMap<>();

    private static final int LIMITE_TEMPO_ENVIO_MS = 5_000;
    private static final int LIMITE_BUFFER_ENVIO_BYTES = 512 * 1024;

    public MesaChatHandler(ObjectMapper objectMapper, RolagemDadosService rolagemDadosService) {
        this.objectMapper = objectMapper;
        this.rolagemDadosService = rolagemDadosService;
    }

    // ── Ciclo de vida da sessão ───────────────────────────────────────────────

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String tableId = extractTableId(session);
        WebSocketSession sessaoSegura = new ConcurrentWebSocketSessionDecorator(
                session, LIMITE_TEMPO_ENVIO_MS, LIMITE_BUFFER_ENVIO_BYTES);
        sessoesComEnvioSerializado.put(session.getId(), sessaoSegura);
        rooms.computeIfAbsent(tableId, k -> new CopyOnWriteArraySet<>()).add(sessaoSegura);

        // Notifica a sala que alguém entrou
        ChatMessage entrada = systemMessage(tableId, "Um aventureiro entrou na sala.");
        broadcast(tableId, entrada, null);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String tableId = extractTableId(session);
        WebSocketSession sessaoSegura = sessoesComEnvioSerializado.remove(session.getId());
        Set<WebSocketSession> room = rooms.get(tableId);
        if (room != null && sessaoSegura != null) {
            room.remove(sessaoSegura);
            if (room.isEmpty()) rooms.remove(tableId);
        }
    }

    // ── Recepção de mensagens ─────────────────────────────────────────────────

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage raw) throws Exception {
        ChatMessage message = objectMapper.readValue(raw.getPayload(), ChatMessage.class);

        // Garante que a mensagem tenha ID e timestamp
        if (message.getId() == null || message.getId().isBlank()) {
            message.setId(UUID.randomUUID().toString());
        }
        if (message.getCreatedAt() == null || message.getCreatedAt().isBlank()) {
            message.setCreatedAt(Instant.now().toString());
        }

        String tableId = message.getTableId();

        if (message.getType() == ChatMessage.Type.ROLAGEM) {
            try {
                aplicarRolagemDoServidor(message);
            } catch (IllegalArgumentException e) {
                enviarParaSessao(session, systemMessage(tableId, e.getMessage()));
                return;
            }
        }

        // TODO: quando IA estiver integrada, verificar mentionedNpcId aqui
        // e gerar uma ChatMessage do tipo NPC_RESPOSTA assincronamente

        broadcast(tableId, message, null);
    }

    private void aplicarRolagemDoServidor(ChatMessage message) {
        ChatMessage.DiceRollResult resultado = rolagemDadosService.rolar(message.getRollRequest());
        message.setDiceResult(resultado);
        message.setRollRequest(null);
        message.setContent(rolagemDadosService.descrever(resultado));
    }

    private void enviarParaSessao(WebSocketSession session, ChatMessage message) {
        WebSocketSession destino = sessoesComEnvioSerializado.getOrDefault(session.getId(), session);
        try {
            destino.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
        } catch (IOException | IllegalStateException ignored) {}
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        session.close(CloseStatus.SERVER_ERROR);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Envia uma mensagem para todos na mesa.
     * @param exclude sessão a excluir do broadcast (null = envia para todos)
     */
    public void broadcast(String tableId, ChatMessage message, WebSocketSession exclude) {
        Set<WebSocketSession> room = rooms.getOrDefault(tableId, Set.of());
        String payload;
        try {
            payload = objectMapper.writeValueAsString(message);
        } catch (IOException e) {
            return;
        }

        for (WebSocketSession s : room) {
            if (s.equals(exclude) || !s.isOpen()) continue;
            try {
                s.sendMessage(new TextMessage(payload));
            } catch (IOException | IllegalStateException ignored) {}
        }
    }

    private String extractTableId(WebSocketSession session) {
        // URI: /ws/mesas/{tableId}
        String path = session.getUri() != null ? session.getUri().getPath() : "";
        String[] parts = path.split("/");
        return parts.length > 0 ? parts[parts.length - 1] : "default";
    }

    private ChatMessage systemMessage(String tableId, String content) {
        ChatMessage msg = new ChatMessage();
        msg.setId(UUID.randomUUID().toString());
        msg.setTableId(tableId);
        msg.setAuthorId("sistema");
        msg.setAuthorName("Sistema");
        msg.setType(ChatMessage.Type.SISTEMA);
        msg.setContent(content);
        msg.setCreatedAt(Instant.now().toString());
        return msg;
    }
}
