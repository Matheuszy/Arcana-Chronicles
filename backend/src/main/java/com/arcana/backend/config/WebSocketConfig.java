package com.arcana.backend.config;

import com.arcana.backend.chat.AutenticacaoHandshakeInterceptor;
import com.arcana.backend.chat.MesaChatHandler;
import com.arcana.backend.chat.imagem.ValidadorImagem;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

/**
 * Registra o handler WebSocket na rota que o frontend usa:
 * ws://localhost:8080/ws/mesas/{tableId}
 *
 * allowedOrigins: liberado para o Vite em dev (porta 5173) e para o
 * frontend servido via Docker/Nginx (porta 4200).
 * Em produção, substituir pelo domínio real.
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final MesaChatHandler mesaChatHandler;
    private final AutenticacaoHandshakeInterceptor autenticacaoHandshakeInterceptor;

    public WebSocketConfig(MesaChatHandler mesaChatHandler,
                           AutenticacaoHandshakeInterceptor autenticacaoHandshakeInterceptor) {
        this.mesaChatHandler = mesaChatHandler;
        this.autenticacaoHandshakeInterceptor = autenticacaoHandshakeInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        DefaultHandshakeHandler handshakeHandler = new DefaultHandshakeHandler();
        handshakeHandler.setSupportedProtocols(AutenticacaoHandshakeInterceptor.PROTOCOLO_AUTENTICACAO);

        registry
            .addHandler(mesaChatHandler, "/ws/mesas/*")
            .setHandshakeHandler(handshakeHandler)
            .addInterceptors(autenticacaoHandshakeInterceptor)
            .setAllowedOrigins("http://localhost:5173", "http://localhost:4200");
    }

    @Bean
    public ServletServerContainerFactoryBean webSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxTextMessageBufferSize(ValidadorImagem.LIMITE_TAMANHO_MENSAGEM_BYTES);
        return container;
    }
}
