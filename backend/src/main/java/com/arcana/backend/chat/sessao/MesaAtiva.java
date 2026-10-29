package com.arcana.backend.chat.sessao;

import com.arcana.backend.chat.model.ChatMessage;
import com.arcana.backend.chat.model.Presenca;
import com.arcana.backend.table.AcessoMesaDto;
import org.springframework.web.socket.WebSocketSession;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;

public class MesaAtiva {

    public static final int LIMITE_HISTORICO = 200;

    public record Conexao(WebSocketSession sessao, AcessoMesaDto acesso) {}

    private final Long mesaId;
    private final Map<String, Conexao> conexoes = new LinkedHashMap<>();
    private final Deque<ChatMessage> historico = new ArrayDeque<>();
    private ScheduledFuture<?> encerramentoAgendado;

    public MesaAtiva(Long mesaId) {
        this.mesaId = mesaId;
    }

    public Long getMesaId() {
        return mesaId;
    }

    synchronized boolean adicionarConexao(String sessionId, WebSocketSession sessao, AcessoMesaDto acesso) {
        boolean primeiraDoUsuario = !usuarioConectado(acesso.userId());
        conexoes.put(sessionId, new Conexao(sessao, acesso));
        return primeiraDoUsuario;
    }

    synchronized Optional<Conexao> removerConexao(String sessionId) {
        return Optional.ofNullable(conexoes.remove(sessionId));
    }

    synchronized boolean vazia() {
        return conexoes.isEmpty();
    }

    public synchronized boolean usuarioConectado(Long userId) {
        return conexoes.values().stream().anyMatch(c -> c.acesso().userId().equals(userId));
    }

    public synchronized void registrar(ChatMessage mensagem) {
        historico.addLast(mensagem);
        while (historico.size() > LIMITE_HISTORICO) {
            historico.removeFirst();
        }
    }

    public synchronized List<ChatMessage> historico() {
        return List.copyOf(historico);
    }

    public synchronized List<Presenca> presentes() {
        Map<Long, Presenca> porUsuario = new LinkedHashMap<>();
        for (Conexao conexao : conexoes.values()) {
            AcessoMesaDto acesso = conexao.acesso();
            porUsuario.putIfAbsent(acesso.userId(),
                    new Presenca(acesso.userId(), acesso.displayName(), acesso.role()));
        }
        List<Presenca> lista = new ArrayList<>(porUsuario.values());
        lista.sort(Comparator.comparing((Presenca p) -> !"MESTRE".equals(p.role()))
                .thenComparing(Presenca::displayName, String.CASE_INSENSITIVE_ORDER));
        return lista;
    }

    public synchronized void agendarEncerramento(ScheduledFuture<?> tarefa) {
        cancelarEncerramento();
        encerramentoAgendado = tarefa;
    }

    public synchronized boolean cancelarEncerramento() {
        if (encerramentoAgendado == null) {
            return false;
        }
        encerramentoAgendado.cancel(false);
        encerramentoAgendado = null;
        return true;
    }

    public synchronized boolean mestreConectado() {
        return conexoes.values().stream().anyMatch(c -> "MESTRE".equals(c.acesso().role()));
    }

    public synchronized List<WebSocketSession> sessoes() {
        return conexoes.values().stream().map(Conexao::sessao).toList();
    }
}
