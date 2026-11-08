package com.arcana.backend;

import com.arcana.backend.config.exception.RecursoNaoEncontradoException;
import com.arcana.backend.user.model.Usuario;
import com.arcana.backend.user.repository.UsuarioRepositorie;
import com.arcana.backend.user.service.NotasDeAtualizacaoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotasDeAtualizacaoServiceTest {

    @Mock
    private UsuarioRepositorie usuarioRepositorie;

    private NotasDeAtualizacaoService service;

    private Usuario alice;

    @BeforeEach
    void setUp() {
        service = new NotasDeAtualizacaoService(usuarioRepositorie);
        alice = new Usuario("alice", "alice@arcana.com", "hash");
        alice.setId(1L);
    }

    @Test
    void deveInformarQueNenhumaNotaFoiVistaPorContaNova() {
        when(usuarioRepositorie.findById(1L)).thenReturn(Optional.of(alice));

        assertNull(service.ultimaVista(1L).versao());
    }

    @Test
    void deveRegistrarAUltimaVersaoVistaNaConta() {
        when(usuarioRepositorie.findById(1L)).thenReturn(Optional.of(alice));

        assertEquals("1.1.0", service.registrarVista(1L, "1.1.0").versao());
        assertEquals("1.1.0", alice.getUltimaNotaVista());
        verify(usuarioRepositorie).save(alice);
    }

    @Test
    void deveFalharQuandoAContaNaoExiste() {
        when(usuarioRepositorie.findById(9L)).thenReturn(Optional.empty());

        assertThrows(RecursoNaoEncontradoException.class, () -> service.ultimaVista(9L));
    }
}
