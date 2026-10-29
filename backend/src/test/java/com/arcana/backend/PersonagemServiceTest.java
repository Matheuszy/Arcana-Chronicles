package com.arcana.backend;

import com.arcana.backend.character.PersonagemRequestDto;
import com.arcana.backend.character.model.Atributos;
import com.arcana.backend.character.model.Personagem;
import com.arcana.backend.character.model.TipoJogador;
import com.arcana.backend.character.repository.PersonagemRepository;
import com.arcana.backend.character.service.PersonagemService;
import com.arcana.backend.user.model.Usuario;
import com.arcana.backend.user.repository.UsuarioRepositorie;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PersonagemServiceTest {

    @Mock
    private PersonagemRepository personagemRepository;

    @Mock
    private UsuarioRepositorie usuarioRepositorie;

    private PersonagemService service;

    @BeforeEach
    void setUp() {
        service = new PersonagemService(personagemRepository, usuarioRepositorie, new ObjectMapper());
    }

    private Personagem personagemDeAlice() {
        Usuario alice = new Usuario("alice", "alice@arcana.com", "hash");
        alice.setId(1L);

        Personagem personagem = new Personagem();
        personagem.setId(10L);
        personagem.setName("Lyra");
        personagem.setKind(TipoJogador.PLAYER);
        personagem.setLevel(1);
        personagem.setHpMax(10);
        personagem.setHpCurrent(10);
        personagem.setArmorClass(12);
        personagem.setAttributes(new Atributos(10, 10, 10, 10, 10, 10));
        personagem.setOwner(alice);
        return personagem;
    }

    private PersonagemRequestDto dtoValido() {
        return new PersonagemRequestDto(
                "Lyra", TipoJogador.PLAYER, 1, 10, 10, 12,
                new PersonagemRequestDto.AtributosDto(10, 10, 10, 10, 10, 10),
                null, null, null, null, null, null, null
        );
    }

    @Test
    void devePermitirBuscarPersonagemProprio() {
        Personagem personagem = personagemDeAlice();
        when(personagemRepository.findById(10L)).thenReturn(Optional.of(personagem));

        var dto = service.buscarPorId(10L, 1L);

        assertEquals("Lyra", dto.name());
    }

    @Test
    void deveNegarBuscarPersonagemDeOutroUsuario() {
        Personagem personagem = personagemDeAlice();
        when(personagemRepository.findById(10L)).thenReturn(Optional.of(personagem));

        assertThrows(AccessDeniedException.class, () -> service.buscarPorId(10L, 2L));
    }

    @Test
    void deveNegarAtualizarPersonagemDeOutroUsuario() {
        Personagem personagem = personagemDeAlice();
        when(personagemRepository.findById(10L)).thenReturn(Optional.of(personagem));

        assertThrows(AccessDeniedException.class, () -> service.atualizar(10L, dtoValido(), 2L));
        verify(personagemRepository, never()).save(any());
    }

    @Test
    void deveManterAColecaoDePericiasAoAtualizarSemPericias() {
        Personagem personagem = personagemDeAlice();
        personagem.getSkills().add("arcanismo");
        List<String> colecaoOriginal = personagem.getSkills();
        when(personagemRepository.findById(10L)).thenReturn(Optional.of(personagem));
        when(personagemRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.atualizar(10L, dtoValido(), 1L);

        assertSame(colecaoOriginal, personagem.getSkills());
        assertTrue(personagem.getSkills().isEmpty());
        assertDoesNotThrow(() -> personagem.getSkills().add("furtividade"));
    }

    @Test
    void deveNegarDeletarPersonagemDeOutroUsuario() {
        Personagem personagem = personagemDeAlice();
        when(personagemRepository.findById(10L)).thenReturn(Optional.of(personagem));

        assertThrows(AccessDeniedException.class, () -> service.deletar(10L, 2L));
        verify(personagemRepository, never()).delete(any());
    }
}
