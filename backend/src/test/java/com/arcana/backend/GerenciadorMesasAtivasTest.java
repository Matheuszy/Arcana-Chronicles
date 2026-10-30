package com.arcana.backend;

import com.arcana.backend.chat.sessao.GerenciadorMesasAtivas;
import com.arcana.backend.table.AcessoMesaDto;
import com.arcana.backend.table.model.StatusMesa;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.WebSocketSession;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class GerenciadorMesasAtivasTest {

    private static final AcessoMesaDto ARIA = new AcessoMesaDto(1L, 20L, "Aria", "JOGADOR", StatusMesa.EM_ANDAMENTO);

    @Test
    void deveInformarSeOUsuarioEstaConectadoEmAlgumaMesa() {
        GerenciadorMesasAtivas gerenciador = new GerenciadorMesasAtivas();
        assertFalse(gerenciador.usuarioEmSessao(20L));

        gerenciador.entrar(ARIA, "sessao-1", mock(WebSocketSession.class));
        assertTrue(gerenciador.usuarioEmSessao(20L));
        assertFalse(gerenciador.usuarioEmSessao(30L));

        gerenciador.sair(1L, "sessao-1");
        assertFalse(gerenciador.usuarioEmSessao(20L));
    }
}
