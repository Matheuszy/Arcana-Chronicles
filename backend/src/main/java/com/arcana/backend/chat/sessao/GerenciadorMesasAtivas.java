package com.arcana.backend.chat.sessao;

import com.arcana.backend.table.AcessoMesaDto;
import com.arcana.backend.table.SessoesAtivasDeMesa;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

@Component
public class GerenciadorMesasAtivas implements SessoesAtivasDeMesa {

    public record Entrada(MesaAtiva mesa, boolean primeiraConexaoDoUsuario) {}

    public record Saida(MesaAtiva mesa, AcessoMesaDto acesso, boolean usuarioAindaConectado, boolean mesaEncerrada) {}

    private final Map<Long, MesaAtiva> mesas = new ConcurrentHashMap<>();

    public Entrada entrar(AcessoMesaDto acesso, String sessionId, WebSocketSession sessao) {
        boolean[] primeira = new boolean[1];
        MesaAtiva mesa = mesas.compute(acesso.mesaId(), (id, existente) -> {
            MesaAtiva ativa = existente != null ? existente : new MesaAtiva(id, acesso.statusMesa());
            primeira[0] = ativa.adicionarConexao(sessionId, sessao, acesso);
            return ativa;
        });
        return new Entrada(mesa, primeira[0]);
    }

    public Optional<Saida> sair(Long mesaId, String sessionId) {
        Saida[] saida = new Saida[1];
        mesas.computeIfPresent(mesaId, (id, mesa) -> {
            mesa.removerConexao(sessionId).ifPresent(conexao -> {
                AcessoMesaDto acesso = conexao.acesso();
                saida[0] = new Saida(mesa, acesso, mesa.usuarioConectado(acesso.userId()), mesa.vazia());
            });
            if (mesa.vazia()) {
                mesa.cancelarEncerramento();
                return null;
            }
            return mesa;
        });
        return Optional.ofNullable(saida[0]);
    }

    public Optional<MesaAtiva> encerrar(Long mesaId) {
        return encerrarSe(mesaId, mesa -> true);
    }

    public Optional<MesaAtiva> encerrarSe(Long mesaId, Predicate<MesaAtiva> condicao) {
        MesaAtiva[] encerrada = new MesaAtiva[1];
        mesas.computeIfPresent(mesaId, (id, mesa) -> {
            if (!condicao.test(mesa)) {
                return mesa;
            }
            mesa.cancelarEncerramento();
            encerrada[0] = mesa;
            return null;
        });
        return Optional.ofNullable(encerrada[0]);
    }

    public Optional<MesaAtiva> buscar(Long mesaId) {
        return Optional.ofNullable(mesas.get(mesaId));
    }

    @Override
    public boolean possuiSessaoAtiva(Long mesaId) {
        return mesas.containsKey(mesaId);
    }
}
