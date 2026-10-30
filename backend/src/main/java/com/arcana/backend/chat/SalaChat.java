package com.arcana.backend.chat;

import com.arcana.backend.chat.model.Canal;
import com.arcana.backend.chat.model.ChatMessage;
import com.arcana.backend.chat.sala.ComandoSala;
import com.arcana.backend.chat.sala.NotaCompartilhada;
import com.arcana.backend.chat.sala.SalaService;
import com.arcana.backend.chat.sala.StatusDaMesa;
import com.arcana.backend.chat.sala.StatusDto;
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
public class SalaChat {

    public static final Set<ChatMessage.Type> COMANDOS = EnumSet.of(
            ChatMessage.Type.NOTA_SALVAR,
            ChatMessage.Type.NOTA_COMPARTILHAR,
            ChatMessage.Type.STATUS_DEFINIR,
            ChatMessage.Type.STATUS_REMOVER,
            ChatMessage.Type.ITEM_CRIAR,
            ChatMessage.Type.ITEM_REMOVER);

    private final SalaService salaService;
    private final ObjectMapper objectMapper;

    public SalaChat(SalaService salaService, ObjectMapper objectMapper) {
        this.salaService = salaService;
        this.objectMapper = objectMapper;
    }

    public void executar(MesaAtiva mesa, AcessoMesaDto acesso, ChatMessage mensagem) {
        ComandoSala comando = mensagem.getComandoSala() == null
                ? new ComandoSala(null, null, null, null, null, null)
                : mensagem.getComandoSala();

        switch (mensagem.getType()) {
            case NOTA_SALVAR -> {
                String nota = salaService.salvarNota(mesa, acesso, mensagem.getContent());
                notificarNota(mesa, acesso.userId(), nota);
            }
            case NOTA_COMPARTILHAR -> {
                NotaCompartilhada compartilhada = salaService.compartilharNota(mesa, acesso, comando);
                notificarNotaCompartilhada(mesa, acesso, compartilhada);
            }
            case STATUS_DEFINIR -> {
                StatusDto.Registro registro = salaService.definirStatus(mesa, acesso, comando);
                anunciarStatus(mesa, registro);
                notificarStatus(mesa);
            }
            case STATUS_REMOVER -> {
                StatusDto.Registro registro = salaService.removerStatus(mesa, acesso, comando);
                anunciarStatus(mesa, registro);
                notificarStatus(mesa);
            }
            case ITEM_CRIAR -> {
                salaService.criarItem(mesa, acesso, comando);
                notificarItens(mesa);
            }
            case ITEM_REMOVER -> {
                salaService.removerItem(mesa, acesso, comando);
                notificarItens(mesa);
            }
            default -> throw new IllegalArgumentException("Comando desconhecido.");
        }
    }

    public void sincronizar(MesaAtiva mesa, WebSocketSession sessao, AcessoMesaDto acesso) {
        ChatMessage nota = novoEvento(mesa, ChatMessage.Type.NOTA);
        nota.setContent(salaService.notaDe(mesa, acesso.userId()));
        enviar(sessao, nota);

        ChatMessage status = novoEvento(mesa, ChatMessage.Type.STATUS);
        status.setStatus(salaService.statusPara(mesa, acesso));
        enviar(sessao, status);

        ChatMessage itens = novoEvento(mesa, ChatMessage.Type.ITENS);
        itens.setItens(salaService.itens(mesa));
        enviar(sessao, itens);
    }

    public void aoJogadorSair(MesaAtiva mesa, Long jogadorId) {
        List<StatusDto.Registro> removidos = salaService.removerStatusDoJogador(mesa, jogadorId);
        if (removidos.isEmpty()) return;
        removidos.forEach(registro -> anunciarStatus(mesa, registro));
        notificarStatus(mesa);
    }

    private void anunciarStatus(MesaAtiva mesa, StatusDto.Registro registro) {
        if (registro.npc()) return;

        String texto = StatusDaMesa.DEFINIDO.equals(registro.acao())
                ? "Status de " + registro.nome() + ": " + registro.texto() + "."
                : "Status de " + registro.nome() + " removido (" + registro.texto() + ").";
        ChatMessage aviso = novoEvento(mesa, ChatMessage.Type.SISTEMA);
        aviso.setCanal(Canal.CONVERSA);
        aviso.setContent(texto);
        mesa.registrar(aviso);
        mesa.sessoes().forEach(sessao -> enviar(sessao, aviso));
    }

    private void notificarNota(MesaAtiva mesa, Long userId, String nota) {
        ChatMessage evento = novoEvento(mesa, ChatMessage.Type.NOTA);
        evento.setContent(nota);
        mesa.sessoesDoUsuario(userId).forEach(sessao -> enviar(sessao, evento));
    }

    private void notificarNotaCompartilhada(MesaAtiva mesa, AcessoMesaDto autor, NotaCompartilhada compartilhada) {
        ChatMessage recebida = novoEvento(mesa, ChatMessage.Type.NOTA_RECEBIDA);
        recebida.setAuthorId(String.valueOf(autor.userId()));
        recebida.setAuthorName(autor.displayName());
        recebida.setContent(compartilhada.trecho());
        compartilhada.destinatarios()
                .forEach(id -> mesa.sessoesDoUsuario(id).forEach(sessao -> enviar(sessao, recebida)));

        ChatMessage confirmacao = novoEvento(mesa, ChatMessage.Type.NOTA_COMPARTILHADA);
        confirmacao.setContent(String.join(", ", compartilhada.nomes()));
        mesa.sessoesDoUsuario(autor.userId()).forEach(sessao -> enviar(sessao, confirmacao));
    }

    private void notificarStatus(MesaAtiva mesa) {
        for (MesaAtiva.Conexao conexao : mesa.conexoes()) {
            ChatMessage evento = novoEvento(mesa, ChatMessage.Type.STATUS);
            evento.setStatus(salaService.statusPara(mesa, conexao.acesso()));
            enviar(conexao.sessao(), evento);
        }
    }

    private void notificarItens(MesaAtiva mesa) {
        ChatMessage evento = novoEvento(mesa, ChatMessage.Type.ITENS);
        evento.setItens(salaService.itens(mesa));
        mesa.sessoes().forEach(sessao -> enviar(sessao, evento));
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
