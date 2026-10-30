package com.arcana.backend.chat.sessao;

import com.arcana.backend.chat.combate.CombateDaMesa;
import com.arcana.backend.chat.desafio.DesafiosDaMesa;
import com.arcana.backend.chat.model.Canal;
import com.arcana.backend.chat.model.ChatMessage;
import com.arcana.backend.chat.model.Presenca;
import com.arcana.backend.chat.sala.ItensDaMesa;
import com.arcana.backend.chat.sala.NotasDaMesa;
import com.arcana.backend.chat.sala.StatusDaMesa;
import com.arcana.backend.table.AcessoMesaDto;
import com.arcana.backend.table.model.StatusMesa;
import org.springframework.web.socket.WebSocketSession;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;

public class MesaAtiva {

    public static final int LIMITE_HISTORICO = 200;
    public static final int LIMITE_IMAGENS = 10;

    public record Conexao(WebSocketSession sessao, AcessoMesaDto acesso) {}

    private final Long mesaId;
    private final Map<String, Conexao> conexoes = new LinkedHashMap<>();
    private final Map<Canal, Deque<ChatMessage>> historicos = new EnumMap<>(Canal.class);
    private final DesafiosDaMesa desafios = new DesafiosDaMesa();
    private final CombateDaMesa combate = new CombateDaMesa();
    private final NotasDaMesa notas = new NotasDaMesa();
    private final StatusDaMesa statusDeFichas = new StatusDaMesa();
    private final ItensDaMesa itens = new ItensDaMesa();
    private ScheduledFuture<?> encerramentoAgendado;
    private StatusMesa status;

    public MesaAtiva(Long mesaId, StatusMesa status) {
        this.mesaId = mesaId;
        this.status = status;
    }

    public Long getMesaId() {
        return mesaId;
    }

    public DesafiosDaMesa desafios() {
        return desafios;
    }

    public CombateDaMesa combate() {
        return combate;
    }

    public NotasDaMesa notas() {
        return notas;
    }

    public StatusDaMesa statusDeFichas() {
        return statusDeFichas;
    }

    public ItensDaMesa itens() {
        return itens;
    }

    public synchronized StatusMesa status() {
        return status;
    }

    public synchronized StatusMesa alterarStatus(StatusMesa novoStatus) {
        StatusMesa anterior = status;
        status = novoStatus;
        return anterior;
    }

    public synchronized boolean pausada() {
        return status == StatusMesa.PAUSADA;
    }

    synchronized boolean adicionarConexao(String sessionId, WebSocketSession sessao, AcessoMesaDto acesso) {
        boolean primeiraDoUsuario = !usuarioConectado(acesso.userId());
        conexoes.put(sessionId, new Conexao(sessao, acesso));
        return primeiraDoUsuario;
    }

    synchronized Optional<Conexao> removerConexao(String sessionId) {
        return Optional.ofNullable(conexoes.remove(sessionId));
    }

    synchronized List<WebSocketSession> removerConexoesDoUsuario(Long userId) {
        List<WebSocketSession> removidas = new ArrayList<>();
        conexoes.values().removeIf(conexao -> {
            boolean doUsuario = conexao.acesso().userId().equals(userId);
            if (doUsuario) {
                removidas.add(conexao.sessao());
            }
            return doUsuario;
        });
        return removidas;
    }

    synchronized boolean vazia() {
        return conexoes.isEmpty();
    }

    public synchronized boolean usuarioConectado(Long userId) {
        return conexoes.values().stream().anyMatch(c -> c.acesso().userId().equals(userId));
    }

    public synchronized void registrar(ChatMessage mensagem) {
        Deque<ChatMessage> historico = historicos.computeIfAbsent(mensagem.getCanal(), canal -> new ArrayDeque<>());
        historico.addLast(mensagem);
        int limite = mensagem.getCanal() == Canal.IMAGENS ? LIMITE_IMAGENS : LIMITE_HISTORICO;
        while (historico.size() > limite) {
            historico.removeFirst();
        }
    }

    public synchronized List<ChatMessage> historico(Canal canal) {
        return List.copyOf(historicos.getOrDefault(canal, new ArrayDeque<>()));
    }

    public synchronized Map<Canal, List<ChatMessage>> historicoPorCanal() {
        Map<Canal, List<ChatMessage>> copia = new EnumMap<>(Canal.class);
        for (Canal canal : Canal.values()) {
            copia.put(canal, historico(canal));
        }
        return copia;
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

    public synchronized List<WebSocketSession> sessoesDoUsuario(Long userId) {
        return conexoes.values().stream()
                .filter(conexao -> conexao.acesso().userId().equals(userId))
                .map(Conexao::sessao)
                .toList();
    }

    public synchronized List<Conexao> conexoes() {
        return List.copyOf(conexoes.values());
    }

    public synchronized List<WebSocketSession> sessoes() {
        return conexoes.values().stream().map(Conexao::sessao).toList();
    }
}
