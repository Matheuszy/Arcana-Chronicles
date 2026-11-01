package com.arcana.backend.chat;

import com.arcana.backend.chat.combate.ComandoCombate;
import com.arcana.backend.chat.combate.CombateDto;
import com.arcana.backend.chat.combate.CombateService;
import com.arcana.backend.chat.model.ChatMessage;
import com.arcana.backend.chat.sessao.MesaAtiva;
import com.arcana.backend.table.AcessoMesaDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Component
public class CombateChat {

    public static final Set<ChatMessage.Type> COMANDOS = EnumSet.of(
            ChatMessage.Type.COMBATE_INICIAR,
            ChatMessage.Type.COMBATE_ROLAR,
            ChatMessage.Type.COMBATE_ROLAR_MONSTROS,
            ChatMessage.Type.COMBATE_LIBERAR_REROLL,
            ChatMessage.Type.COMBATE_PASSAR_TURNO,
            ChatMessage.Type.COMBATE_ENCERRAR);

    private final CombateService combateService;
    private final ObjectMapper objectMapper;

    public CombateChat(CombateService combateService, ObjectMapper objectMapper) {
        this.combateService = combateService;
        this.objectMapper = objectMapper;
    }

    public void executar(MesaAtiva mesa, AcessoMesaDto acesso, ChatMessage mensagem) {
        ComandoCombate comando = mensagem.getComandoCombate() == null
                ? new ComandoCombate(null, null, null, null, null)
                : mensagem.getComandoCombate();
        CombateDto antes = combateService.estado(mesa).orElse(null);

        switch (mensagem.getType()) {
            case COMBATE_INICIAR -> combateService.iniciar(mesa, acesso, comando.jogadores(),
                    comando.monstros());
            case COMBATE_ROLAR -> combateService.rolarIniciativa(mesa, acesso, comando.modo(),
                    comando.somarProficiencia());
            case COMBATE_ROLAR_MONSTROS -> combateService.rolarIniciativaDosMonstros(mesa, acesso,
                    comando.entradas(), comando.modo(), comando.somarProficiencia());
            case COMBATE_LIBERAR_REROLL -> combateService.liberarRerolagem(mesa, acesso, comando.entradas());
            case COMBATE_PASSAR_TURNO -> combateService.passarTurno(mesa, acesso);
            case COMBATE_ENCERRAR -> combateService.encerrar(mesa, acesso);
            default -> throw new IllegalArgumentException("Comando de combate desconhecido.");
        }

        publicar(mesa, antes);
    }

    public void sincronizar(MesaAtiva mesa, WebSocketSession sessao) {
        enviar(sessao, eventoDoCombate(mesa, combateService.estado(mesa).orElse(null)));
    }

    public void aoJogadorSair(MesaAtiva mesa, Long jogadorId) {
        CombateDto antes = combateService.estado(mesa).orElse(null);
        combateService.aoJogadorSair(mesa, jogadorId);
        publicar(mesa, antes);
    }

    public boolean ativo(MesaAtiva mesa) {
        return combateService.estado(mesa).isPresent();
    }

    public boolean podeRolarLivremente(MesaAtiva mesa, Long userId) {
        return combateService.podeRolarLivremente(mesa, userId);
    }

    private void publicar(MesaAtiva mesa, CombateDto antes) {
        CombateDto depois = combateService.estado(mesa).orElse(null);
        if (Objects.equals(antes, depois)) return;

        ChatMessage evento = eventoDoCombate(mesa, depois);
        mesa.sessoes().forEach(sessao -> enviar(sessao, evento));
    }

    private ChatMessage eventoDoCombate(MesaAtiva mesa, CombateDto combate) {
        ChatMessage evento = novoEvento(mesa, ChatMessage.Type.COMBATE);
        evento.setCombate(combate);
        return evento;
    }

    private void enviar(WebSocketSession sessao, ChatMessage evento) {
        if (!sessao.isOpen()) return;
        try {
            sessao.sendMessage(new TextMessage(objectMapper.writeValueAsString(evento)));
        } catch (IOException | IllegalStateException ignored) {}
    }

    private ChatMessage novoEvento(MesaAtiva mesa, ChatMessage.Type tipo) {
        ChatMessage evento = new ChatMessage();
        evento.setId(UUID.randomUUID().toString());
        evento.setTableId(String.valueOf(mesa.getMesaId()));
        evento.setAuthorId("sistema");
        evento.setAuthorName("Sistema");
        evento.setType(tipo);
        evento.setCreatedAt(Instant.now().toString());
        return evento;
    }
}
