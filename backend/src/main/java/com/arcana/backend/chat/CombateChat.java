package com.arcana.backend.chat;

import com.arcana.backend.chat.combate.Combate;
import com.arcana.backend.chat.combate.ComandoCombate;
import com.arcana.backend.chat.combate.CombateDto;
import com.arcana.backend.chat.combate.CombateService;
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
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
public class CombateChat {

    public static final Set<ChatMessage.Type> COMANDOS = EnumSet.of(
            ChatMessage.Type.COMBATE_INICIAR,
            ChatMessage.Type.COMBATE_ROLAR,
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
                ? new ComandoCombate(null, null, null)
                : mensagem.getComandoCombate();
        CombateDto antes = combateService.estado(mesa).orElse(null);

        switch (mensagem.getType()) {
            case COMBATE_INICIAR -> combateService.iniciar(mesa, acesso, comando.jogadores());
            case COMBATE_ROLAR -> combateService.rolarIniciativa(mesa, acesso, comando.modo(),
                    comando.somarProficiencia());
            case COMBATE_LIBERAR_REROLL -> combateService.liberarRerolagem(mesa, acesso, comando.jogadores());
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

    public boolean podeRolarLivremente(MesaAtiva mesa, Long userId) {
        return combateService.podeRolarLivremente(mesa, userId);
    }

    private void publicar(MesaAtiva mesa, CombateDto antes) {
        CombateDto depois = combateService.estado(mesa).orElse(null);
        if (Objects.equals(antes, depois)) return;

        ChatMessage evento = eventoDoCombate(mesa, depois);
        mesa.sessoes().forEach(sessao -> enviar(sessao, evento));
        anuncios(antes, depois).forEach(texto -> anunciar(mesa, texto));
    }

    private List<String> anuncios(CombateDto antes, CombateDto depois) {
        if (depois == null) {
            return List.of("O combate terminou. Os chats voltam a ser livres.");
        }

        List<String> textos = new ArrayList<>();
        if (antes == null) {
            textos.add("O mestre iniciou o combate. Quem foi escolhido deve rolar a iniciativa.");
        }
        textos.addAll(rerolagensLiberadas(antes, depois));

        boolean passouParaAndamento = depois.fase() == Combate.Fase.EM_ANDAMENTO
                && (antes == null || antes.fase() != Combate.Fase.EM_ANDAMENTO);
        boolean ordemMudou = depois.fase() == Combate.Fase.EM_ANDAMENTO
                && antes != null && antes.fase() == Combate.Fase.EM_ANDAMENTO
                && !ordemDe(antes).equals(ordemDe(depois));
        if (passouParaAndamento || ordemMudou) {
            textos.add(listaDeIniciativa(depois));
        }

        boolean turnoMudou = depois.fase() == Combate.Fase.EM_ANDAMENTO
                && (passouParaAndamento || antes.rodada() != depois.rodada()
                || !Objects.equals(antes.turnoDe(), depois.turnoDe()));
        if (turnoMudou) {
            textos.add("Rodada " + depois.rodada() + ": turno de " + nomeDoTurno(depois) + ".");
        }
        return textos;
    }

    private List<String> rerolagensLiberadas(CombateDto antes, CombateDto depois) {
        if (antes == null) return List.of();
        return depois.participantes().stream()
                .filter(CombateDto.EntradaDto::rerolagemLiberada)
                .filter(entrada -> antes.participantes().stream()
                        .noneMatch(anterior -> anterior.jogadorId().equals(entrada.jogadorId())
                                && anterior.rerolagemLiberada()))
                .map(entrada -> "O mestre liberou uma nova rolagem de iniciativa para "
                        + entrada.nomePersonagem() + ".")
                .toList();
    }

    private List<Long> ordemDe(CombateDto combate) {
        return combate.participantes().stream().map(CombateDto.EntradaDto::jogadorId).toList();
    }

    private String listaDeIniciativa(CombateDto combate) {
        List<CombateDto.EntradaDto> participantes = combate.participantes();
        String linhas = IntStream.range(0, participantes.size())
                .mapToObj(i -> (i + 1) + ". " + participantes.get(i).nomePersonagem()
                        + " (" + participantes.get(i).nomeJogador() + "): "
                        + participantes.get(i).iniciativa().total())
                .collect(Collectors.joining("\n"));
        return "Ordem de iniciativa:\n" + linhas;
    }

    private String nomeDoTurno(CombateDto combate) {
        return combate.participantes().stream()
                .filter(entrada -> entrada.jogadorId().equals(combate.turnoDe()))
                .map(entrada -> entrada.nomePersonagem() + " (" + entrada.nomeJogador() + ")")
                .findFirst()
                .orElse("-");
    }

    private void anunciar(MesaAtiva mesa, String texto) {
        ChatMessage aviso = novoEvento(mesa, ChatMessage.Type.SISTEMA);
        aviso.setCanal(Canal.CONVERSA);
        aviso.setContent(texto);
        mesa.registrar(aviso);
        mesa.sessoes().forEach(sessao -> enviar(sessao, aviso));
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
