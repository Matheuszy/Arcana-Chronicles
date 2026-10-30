package com.arcana.backend.chat;

import com.arcana.backend.chat.desafio.ComandoDesafio;
import com.arcana.backend.chat.desafio.DesafioDto;
import com.arcana.backend.chat.desafio.DesafioService;
import com.arcana.backend.chat.desafio.NovoDesafio;
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
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
public class DesafioChat {

    public static final Set<ChatMessage.Type> COMANDOS = EnumSet.of(
            ChatMessage.Type.DESAFIO_CRIAR,
            ChatMessage.Type.DESAFIO_ROLAR,
            ChatMessage.Type.DESAFIO_TENTATIVA_EXTRA,
            ChatMessage.Type.DESAFIO_ENCERRAR_FALHA,
            ChatMessage.Type.DESAFIO_CANCELAR,
            ChatMessage.Type.DESAFIO_MENSAGEM);

    private final DesafioService desafioService;
    private final ObjectMapper objectMapper;

    public DesafioChat(DesafioService desafioService, ObjectMapper objectMapper) {
        this.desafioService = desafioService;
        this.objectMapper = objectMapper;
    }

    public void executar(MesaAtiva mesa, AcessoMesaDto acesso, ChatMessage mensagem) {
        ComandoDesafio comando = mensagem.getComandoDesafio();
        if (comando == null) {
            throw new IllegalArgumentException("Comando de desafio ausente.");
        }

        String desafioId = switch (mensagem.getType()) {
            case DESAFIO_CRIAR -> desafioService.criar(mesa, acesso,
                    new NovoDesafio(comando.cd(), comando.descricao(), comando.jogadores())).id();
            case DESAFIO_ROLAR -> "MESTRE".equals(acesso.role())
                    ? desafioService.rolarMonstros(mesa, acesso, comando.desafioId(), comando.alvos(),
                    comando.escolha(), comando.modo()).id()
                    : desafioService.rolar(mesa, acesso, comando.escolha()).desafio().id();
            case DESAFIO_TENTATIVA_EXTRA -> desafioService.liberarTentativaExtra(
                    mesa, acesso, comando.desafioId(), comando.alvo(), comando.modo()).id();
            case DESAFIO_ENCERRAR_FALHA -> desafioService.encerrarComFalha(
                    mesa, acesso, comando.desafioId(), comando.alvo()).id();
            case DESAFIO_CANCELAR -> desafioService.cancelar(
                    mesa, acesso, comando.desafioId(), comando.alvo()).id();
            case DESAFIO_MENSAGEM -> desafioService.enviarMensagem(
                    mesa, acesso, comando.desafioId(), comando.destino(), comando.alvo(), comando.texto())
                    .desafio().id();
            default -> throw new IllegalArgumentException("Comando de desafio desconhecido.");
        };

        notificar(mesa, desafioId);
    }

    public void sincronizar(MesaAtiva mesa, WebSocketSession sessao, Long userId) {
        ChatMessage evento = novoEvento(mesa, ChatMessage.Type.DESAFIOS);
        evento.setDesafios(desafioService.visiveisPara(mesa, userId));
        enviar(sessao, evento);
    }

    public void aoJogadorSair(MesaAtiva mesa, Long jogadorId) {
        desafioService.cancelarDoJogador(mesa, jogadorId).forEach(desafio -> notificar(mesa, desafio.id()));
    }

    public void aoCombateEncerrado(MesaAtiva mesa) {
        desafioService.cancelarDoCombate(mesa).forEach(desafio -> notificar(mesa, desafio.id()));
    }

    public boolean jogadorBloqueado(MesaAtiva mesa, Long userId) {
        return desafioService.jogadorBloqueado(mesa, userId);
    }

    private void notificar(MesaAtiva mesa, String desafioId) {
        for (Long userId : desafioService.envolvidos(mesa, desafioId)) {
            List<WebSocketSession> sessoes = mesa.sessoesDoUsuario(userId);
            if (sessoes.isEmpty()) continue;

            ChatMessage evento = novoEvento(mesa, ChatMessage.Type.DESAFIO);
            evento.setDesafio(desafioService.visaoPara(mesa, desafioId, userId));
            sessoes.forEach(sessao -> enviar(sessao, evento));
        }
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
