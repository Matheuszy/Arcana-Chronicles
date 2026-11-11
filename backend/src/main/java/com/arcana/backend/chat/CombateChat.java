package com.arcana.backend.chat;

import com.arcana.backend.chat.combate.AdicaoAoCombate;
import com.arcana.backend.chat.combate.ComandoCombate;
import com.arcana.backend.chat.combate.CombateDto;
import com.arcana.backend.chat.combate.CombateService;
import com.arcana.backend.chat.combate.EntradaNoCombateDto;
import com.arcana.backend.chat.combate.ReacaoDeclarada;
import com.arcana.backend.chat.model.Canal;
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
import java.util.Optional;
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
            ChatMessage.Type.COMBATE_ENCERRAR,
            ChatMessage.Type.COMBATE_ADICIONAR,
            ChatMessage.Type.COMBATE_REVELAR,
            ChatMessage.Type.COMBATE_REAGIR,
            ChatMessage.Type.COMBATE_ENCERRAR_REACAO);

    private final CombateService combateService;
    private final ObjectMapper objectMapper;

    public CombateChat(CombateService combateService, ObjectMapper objectMapper) {
        this.combateService = combateService;
        this.objectMapper = objectMapper;
    }

    public void executar(MesaAtiva mesa, AcessoMesaDto acesso, ChatMessage mensagem) {
        ComandoCombate comando = mensagem.getComandoCombate() == null
                ? ComandoCombate.vazio()
                : mensagem.getComandoCombate();
        List<CombateDto> antes = combateService.estados(mesa);
        AdicaoAoCombate adicao = null;
        ReacaoDeclarada reacao = null;

        switch (mensagem.getType()) {
            case COMBATE_INICIAR -> combateService.iniciar(mesa, acesso, comando.nome(), comando.jogadores(),
                    comando.monstros());
            case COMBATE_ROLAR -> combateService.rolarIniciativa(mesa, acesso, comando.modo());
            case COMBATE_ROLAR_MONSTROS -> combateService.rolarIniciativaDosMonstros(mesa, acesso,
                    comando.combateId(), comando.entradas(), comando.modo());
            case COMBATE_LIBERAR_REROLL -> combateService.liberarRerolagem(mesa, acesso, comando.combateId(),
                    comando.entradas());
            case COMBATE_PASSAR_TURNO -> combateService.passarTurno(mesa, acesso, comando.combateId());
            case COMBATE_ENCERRAR -> combateService.encerrar(mesa, acesso, comando.combateId());
            case COMBATE_ADICIONAR -> adicao = combateService.adicionar(mesa, acesso, comando.combateId(),
                    comando.jogadores(), comando.monstros());
            case COMBATE_REVELAR -> adicao = new AdicaoAoCombate(combateService.revelar(mesa, acesso,
                    comando.combateId(), comando.entradas()), Set.of());
            case COMBATE_REAGIR -> reacao = combateService.reagir(mesa, acesso, comando.combateId(),
                    comando.entradaId(), comando.tipoReacao(), comando.descricao());
            case COMBATE_ENCERRAR_REACAO -> combateService.encerrarReacao(mesa, acesso, comando.combateId(),
                    comando.entradaId());
            default -> throw new IllegalArgumentException("Comando de combate desconhecido.");
        }

        publicar(mesa, antes);
        if (adicao != null && !adicao.anuncio().nomes().isEmpty()) {
            anunciarEntrada(mesa, adicao);
        }
        if (reacao != null) {
            publicarReacao(mesa, acesso, reacao);
        }
    }

    public Optional<String> rotuloDaReacaoAberta(MesaAtiva mesa, Long userId) {
        return combateService.reacaoAbertaDe(mesa, userId)
                .map(reacao -> "Reação de " + reacao.nomePersonagem() + " · " + reacao.reacao().rotulo());
    }

    public void publicandoMudancas(MesaAtiva mesa, Runnable acao) {
        List<CombateDto> antes = combateService.estados(mesa);
        try {
            acao.run();
        } finally {
            publicar(mesa, antes);
        }
    }

    public void sincronizar(MesaAtiva mesa, WebSocketSession sessao, AcessoMesaDto acesso) {
        enviar(sessao, eventoDosCombates(mesa, acesso));
    }

    public void aoJogadorSair(MesaAtiva mesa, Long jogadorId) {
        List<CombateDto> antes = combateService.estados(mesa);
        combateService.aoJogadorSair(mesa, jogadorId);
        publicar(mesa, antes);
    }

    public Set<String> combatesAtivos(MesaAtiva mesa) {
        return combateService.combatesAtivos(mesa);
    }

    public boolean podeRolarLivremente(MesaAtiva mesa, Long userId) {
        return combateService.podeRolarLivremente(mesa, userId);
    }

    private void publicar(MesaAtiva mesa, List<CombateDto> antes) {
        if (antes.equals(combateService.estados(mesa))) return;

        for (MesaAtiva.Conexao conexao : mesa.conexoes()) {
            enviar(conexao.sessao(), eventoDosCombates(mesa, conexao.acesso()));
        }
    }

    private void anunciarEntrada(MesaAtiva mesa, AdicaoAoCombate adicao) {
        ChatMessage evento = novoEvento(mesa, ChatMessage.Type.COMBATE_ENTRADA);
        evento.setEntradaNoCombate(adicao.anuncio());
        for (MesaAtiva.Conexao conexao : mesa.conexoes()) {
            if (!adicao.jogadoresQueEntraram().contains(conexao.acesso().userId())) {
                enviar(conexao.sessao(), evento);
            }
        }
    }

    private void publicarReacao(MesaAtiva mesa, AcessoMesaDto acesso, ReacaoDeclarada reacao) {
        ChatMessage mensagem = novoEvento(mesa, ChatMessage.Type.REACAO);
        mensagem.setAuthorId(String.valueOf(acesso.userId()));
        mensagem.setAuthorName(acesso.displayName());
        mensagem.setCanal(Canal.ROLAGENS);
        mensagem.setReacao(reacao.reacao().rotulo());
        mensagem.setContent(reacao.nomePersonagem() + " usa a reação: " + reacao.reacao().rotulo());
        mesa.registrar(mensagem);
        for (MesaAtiva.Conexao conexao : mesa.conexoes()) {
            enviar(conexao.sessao(), mensagem);
        }
    }

    private ChatMessage eventoDosCombates(MesaAtiva mesa, AcessoMesaDto acesso) {
        ChatMessage evento = novoEvento(mesa, ChatMessage.Type.COMBATE);
        evento.setCombates(combateService.visaoDo(mesa, acesso));
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
