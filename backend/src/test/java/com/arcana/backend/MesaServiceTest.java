package com.arcana.backend;

import com.arcana.backend.character.model.Atributos;
import com.arcana.backend.character.model.Personagem;
import com.arcana.backend.character.model.TipoJogador;
import com.arcana.backend.character.repository.PersonagemRepository;
import com.arcana.backend.config.exception.RegraDeNegocioException;
import com.arcana.backend.table.MesaRequestDto;
import com.arcana.backend.table.model.Mesa;
import com.arcana.backend.table.model.StatusMesa;
import com.arcana.backend.table.repository.MesaRepository;
import com.arcana.backend.table.service.MesaService;
import com.arcana.backend.user.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MesaServiceTest {

    @Mock
    private MesaRepository mesaRepository;

    @Mock
    private PersonagemRepository personagemRepository;

    private MesaService service;

    @BeforeEach
    void setUp() {
        service = new MesaService(mesaRepository, personagemRepository);
    }

    private Personagem personagemDoJogador(Long ownerId, TipoJogador kind) {
        Usuario owner = new Usuario("joao", "joao@arcana.com", "hash");
        owner.setId(ownerId);

        Personagem personagem = new Personagem();
        personagem.setId(100L);
        personagem.setName("Thorn");
        personagem.setKind(kind);
        personagem.setLevel(1);
        personagem.setHpMax(10);
        personagem.setHpCurrent(10);
        personagem.setArmorClass(10);
        personagem.setAttributes(new Atributos(10, 10, 10, 10, 10, 10));
        personagem.setOwner(owner);
        return personagem;
    }

    @Test
    void deveDerivarNomeDoMestreDoUsuarioAutenticado() {
        MesaRequestDto dto = new MesaRequestDto("Mesa dos Bravos", "Uma aventura épica");
        when(mesaRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var resultado = service.criar(dto, 1L, "gandalf");

        assertEquals("gandalf", resultado.masterName());
        assertEquals(1L, resultado.masterId());
        assertEquals("MESTRE", resultado.participants().get(0).role());
        assertEquals("gandalf", resultado.participants().get(0).displayName());
    }

    @Test
    void deveNegarAlteracaoDeStatusPorQuemNaoEhMestre() {
        Mesa mesa = new Mesa();
        mesa.setId(1L);
        mesa.setMasterId(1L);
        mesa.setStatus(StatusMesa.ABERTA);

        when(mesaRepository.findById(1L)).thenReturn(Optional.of(mesa));

        assertThrows(RuntimeException.class,
                () -> service.atualizarStatus(1L, StatusMesa.EM_ANDAMENTO, 2L));
    }

    @Test
    void deveNegarExclusaoPorQuemNaoEhMestre() {
        Mesa mesa = new Mesa();
        mesa.setId(1L);
        mesa.setMasterId(1L);

        when(mesaRepository.findById(1L)).thenReturn(Optional.of(mesa));

        assertThrows(RuntimeException.class, () -> service.deletar(1L, 2L));
        verify(mesaRepository, never()).delete(any());
    }

    @Test
    void deveNegarEntradaDeNovoJogadorQuandoMesaNaoEstaAberta() {
        Mesa mesa = new Mesa();
        mesa.setId(1L);
        mesa.setMasterId(1L);
        mesa.setStatus(StatusMesa.EM_ANDAMENTO);

        when(mesaRepository.findById(1L)).thenReturn(Optional.of(mesa));

        assertThrows(RegraDeNegocioException.class,
                () -> service.entrar(1L, 2L, "joao", null));
        verify(mesaRepository, never()).save(any());
    }

    @Test
    void deveNegarEntradaComPersonagemDeOutroUsuario() {
        Mesa mesa = new Mesa();
        mesa.setId(1L);
        mesa.setMasterId(1L);
        mesa.setStatus(StatusMesa.ABERTA);

        when(mesaRepository.findById(1L)).thenReturn(Optional.of(mesa));
        when(personagemRepository.findById(100L))
                .thenReturn(Optional.of(personagemDoJogador(99L, TipoJogador.PLAYER)));

        assertThrows(AccessDeniedException.class,
                () -> service.entrar(1L, 2L, "joao", 100L));
        verify(mesaRepository, never()).save(any());
    }

    @Test
    void deveNegarEntradaComPersonagemQueNaoEhDoTipoJogador() {
        Mesa mesa = new Mesa();
        mesa.setId(1L);
        mesa.setMasterId(1L);
        mesa.setStatus(StatusMesa.ABERTA);

        when(mesaRepository.findById(1L)).thenReturn(Optional.of(mesa));
        when(personagemRepository.findById(100L))
                .thenReturn(Optional.of(personagemDoJogador(2L, TipoJogador.MONSTER)));

        assertThrows(IllegalArgumentException.class,
                () -> service.entrar(1L, 2L, "joao", 100L));
        verify(mesaRepository, never()).save(any());
    }

    @Test
    void deveNegarSaidaDoMestre() {
        Mesa mesa = new Mesa();
        mesa.setId(1L);
        mesa.setMasterId(1L);

        when(mesaRepository.findById(1L)).thenReturn(Optional.of(mesa));

        assertThrows(RegraDeNegocioException.class, () -> service.sair(1L, 1L));
        verify(mesaRepository, never()).save(any());
    }

    @Test
    void devePermitirJogadorSairDaMesa() {
        Mesa mesa = new Mesa();
        mesa.setId(1L);
        mesa.setMasterId(1L);
        mesa.getParticipants().add(new com.arcana.backend.table.model.Participante(2L, "joao", "JOGADOR", null));

        when(mesaRepository.findById(1L)).thenReturn(Optional.of(mesa));
        when(mesaRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var resultado = service.sair(1L, 2L);

        assertTrue(resultado.participants().isEmpty());
    }

    @Test
    void devePermitirEntradaComPersonagemProprioDoTipoJogador() {
        Mesa mesa = new Mesa();
        mesa.setId(1L);
        mesa.setMasterId(1L);
        mesa.setStatus(StatusMesa.ABERTA);

        when(mesaRepository.findById(1L)).thenReturn(Optional.of(mesa));
        when(personagemRepository.findById(100L))
                .thenReturn(Optional.of(personagemDoJogador(2L, TipoJogador.PLAYER)));
        when(mesaRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var resultado = service.entrar(1L, 2L, "joao", 100L);

        assertEquals(1, resultado.participants().size());
        assertEquals(100L, resultado.participants().get(0).characterId());
    }

    @Test
    void devePermitirMestreAssociarNpcProprio() {
        Mesa mesa = new Mesa();
        mesa.setId(1L);
        mesa.setMasterId(1L);

        when(mesaRepository.findById(1L)).thenReturn(Optional.of(mesa));
        when(personagemRepository.findById(100L))
                .thenReturn(Optional.of(personagemDoJogador(1L, TipoJogador.NPC_FRIENDLY)));
        when(mesaRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var resultado = service.adicionarNpc(1L, 100L, 1L);

        assertTrue(resultado.npcIds().contains(100L));
    }

    @Test
    void deveNegarAssociarNpcPorQuemNaoEhMestre() {
        Mesa mesa = new Mesa();
        mesa.setId(1L);
        mesa.setMasterId(1L);

        when(mesaRepository.findById(1L)).thenReturn(Optional.of(mesa));

        assertThrows(AccessDeniedException.class, () -> service.adicionarNpc(1L, 100L, 2L));
        verify(mesaRepository, never()).save(any());
    }

    @Test
    void deveNegarAssociarNpcQuePertenceAOutroUsuario() {
        Mesa mesa = new Mesa();
        mesa.setId(1L);
        mesa.setMasterId(1L);

        when(mesaRepository.findById(1L)).thenReturn(Optional.of(mesa));
        when(personagemRepository.findById(100L))
                .thenReturn(Optional.of(personagemDoJogador(99L, TipoJogador.NPC_FRIENDLY)));

        assertThrows(AccessDeniedException.class, () -> service.adicionarNpc(1L, 100L, 1L));
        verify(mesaRepository, never()).save(any());
    }

    @Test
    void deveNegarAssociarPersonagemDoTipoJogadorComoNpc() {
        Mesa mesa = new Mesa();
        mesa.setId(1L);
        mesa.setMasterId(1L);

        when(mesaRepository.findById(1L)).thenReturn(Optional.of(mesa));
        when(personagemRepository.findById(100L))
                .thenReturn(Optional.of(personagemDoJogador(1L, TipoJogador.PLAYER)));

        assertThrows(IllegalArgumentException.class, () -> service.adicionarNpc(1L, 100L, 1L));
        verify(mesaRepository, never()).save(any());
    }

    @Test
    void devePermitirMestreRemoverNpc() {
        Mesa mesa = new Mesa();
        mesa.setId(1L);
        mesa.setMasterId(1L);
        mesa.getNpcIds().add(100L);

        when(mesaRepository.findById(1L)).thenReturn(Optional.of(mesa));
        when(mesaRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var resultado = service.removerNpc(1L, 100L, 1L);

        assertFalse(resultado.npcIds().contains(100L));
    }
}
