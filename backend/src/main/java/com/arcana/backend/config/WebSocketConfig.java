package com.arcana.backend.config;

import com.arcana.backend.chat.MesaChatHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * Registra o handler WebSocket na rota que o frontend usa:
 * ws://localhost:8080/ws/mesas/{tableId}
 *
 * allowedOrigins: liberado para o Vite em dev (porta 5173).
 * Em produção, substituir pelo domínio real.
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final MesaChatHandler mesaChatHandler;

    public WebSocketConfig(MesaChatHandler mesaChatHandler) {
        this.mesaChatHandler = mesaChatHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry
            .addHandler(mesaChatHandler, "/ws/mesas/*")
            .setAllowedOrigins("http://localhost:5173");
    }
}
