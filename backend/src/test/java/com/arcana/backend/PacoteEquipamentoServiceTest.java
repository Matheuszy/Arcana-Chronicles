package com.arcana.backend;

import com.arcana.backend.character.model.Atributos;
import com.arcana.backend.character.model.PacoteEquipamento;
import com.arcana.backend.character.model.Personagem;
import com.arcana.backend.character.model.TipoJogador;
import com.arcana.backend.character.repository.PacoteEquipamentoRepository;
import com.arcana.backend.character.repository.PersonagemRepository;
import com.arcana.backend.character.service.PacoteEquipamentoService;
import com.arcana.backend.config.exception.RecursoNaoEncontradoException;
import com.arcana.backend.config.exception.RegraDeNegocioException;
import com.arcana.backend.table.model.Mesa;
import com.arcana.backend.table.model.Participante;
import com.arcana.backend.table.model.StatusMesa;
import com.arcana.backend.table.model.StatusSolicitacao;
import com.arcana.backend.table.repository.MesaRepository;
import com.arcana.backend.table.repository.SolicitacaoEntradaRepository;
import com.arcana.backend.user.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PacoteEquipamentoServiceTest {

    private static final PacoteEquipamento EXPLORADOR = new PacoteEquipamento("EXPLORADOR", "Explorador", 4,
            List.of("Mochila", "Saco de dormir", "Tocha (10)"));
    private static final PacoteEquipamento ESTUDIOSO = new PacoteEquipamento("ESTUDIOSO", "Estudioso", 6,
            List.of("Mochila", "Livro de conhecimento", "Faca pequena"));

    @Mock
    private PacoteEquipamentoRepository pacoteRepository;

    @Mock
    private PersonagemRepository personagemRepository;

    @Mock
    private MesaRepository mesaRepository;

    @Mock
    private SolicitacaoEntradaRepository solicitacaoRepository;

    private PacoteEquipamentoService service;

    @BeforeEach
    void setUp() {
        service = new PacoteEquipamentoService(pacoteRepository, personagemRepository, mesaRepository,
                solicitacaoRepository);
    }

    private Personagem fichaLivre() {
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

    private Mesa mesaComParticipante(StatusMesa status) {
        Mesa mesa = new Mesa();
        mesa.setId(5L);
        mesa.setStatus(status);
        mesa.getParticipants().add(new Participante(1L, "alice", "JOGADOR", 10L));
        return mesa;
    }

    private void prepararFichaLivre(Personagem personagem) {
        when(personagemRepository.findById(10L)).thenReturn(Optional.of(personagem));
        when(mesaRepository.findByParticipantsCharacterId(10L)).thenReturn(List.of());
        when(solicitacaoRepository.existsByCharacterIdAndStatus(10L, StatusSolicitacao.PENDENTE)).thenReturn(false);
        lenient().when(personagemRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void deveAplicarPacoteEmFichaLivreRegistrandoOsItensQueVieramDele() {
        Personagem personagem = fichaLivre();
        personagem.getItens().add("Amuleto da avó");
        prepararFichaLivre(personagem);
        when(pacoteRepository.findById("EXPLORADOR")).thenReturn(Optional.of(EXPLORADOR));

        var resposta = service.aplicar(10L, 1L, " explorador ");

        assertEquals(List.of("Amuleto da avó", "Mochila", "Saco de dormir", "Tocha (10)"), resposta.items());
        assertEquals("EXPLORADOR", resposta.pacoteEquipamento());
        assertEquals(List.of("Mochila", "Saco de dormir", "Tocha (10)"), resposta.itensDoPacote());
    }

    @Test
    void deveTrocarPacoteMantendoItensManuaisESemDevolverItensRemovidos() {
        Personagem personagem = fichaLivre();
        personagem.getItens().addAll(List.of("Amuleto da avó", "Mochila", "Tocha (10)"));
        personagem.setPacoteEquipamento("EXPLORADOR");
        personagem.getItensDoPacote().addAll(EXPLORADOR.getItens());
        prepararFichaLivre(personagem);
        when(pacoteRepository.findById("ESTUDIOSO")).thenReturn(Optional.of(ESTUDIOSO));

        var resposta = service.aplicar(10L, 1L, "ESTUDIOSO");

        assertEquals(List.of("Amuleto da avó", "Mochila", "Livro de conhecimento", "Faca pequena"), resposta.items());
        assertEquals("ESTUDIOSO", resposta.pacoteEquipamento());
    }

    @Test
    void deveRecusarReaplicarOMesmoPacote() {
        Personagem personagem = fichaLivre();
        personagem.setPacoteEquipamento("EXPLORADOR");
        prepararFichaLivre(personagem);
        when(pacoteRepository.findById("EXPLORADOR")).thenReturn(Optional.of(EXPLORADOR));

        assertThrows(IllegalStateException.class, () -> service.aplicar(10L, 1L, "EXPLORADOR"));
        verify(personagemRepository, never()).save(any());
    }

    @Test
    void deveRecusarPacoteQueFariaAFichaPassarDoLimiteDeItens() {
        Personagem personagem = fichaLivre();
        personagem.getItens().addAll(Collections.nCopies(48, "Pedra"));
        prepararFichaLivre(personagem);
        when(pacoteRepository.findById("EXPLORADOR")).thenReturn(Optional.of(EXPLORADOR));

        var erro = assertThrows(IllegalStateException.class, () -> service.aplicar(10L, 1L, "EXPLORADOR"));

        assertEquals("Com o pacote Explorador a ficha passaria de 50 itens. Remova alguns itens antes.", erro.getMessage());
        assertEquals(48, personagem.getItens().size());
    }

    @Test
    void deveBloquearPacoteEmFichaDeParticipanteDeMesa() {
        when(personagemRepository.findById(10L)).thenReturn(Optional.of(fichaLivre()));
        when(mesaRepository.findByParticipantsCharacterId(10L)).thenReturn(List.of(mesaComParticipante(StatusMesa.ABERTA)));

        assertThrows(RegraDeNegocioException.class, () -> service.aplicar(10L, 1L, "EXPLORADOR"));
        verifyNoInteractions(pacoteRepository);
    }

    @Test
    void deveBloquearPacoteEmFichaComSolicitacaoDeEntradaPendente() {
        when(personagemRepository.findById(10L)).thenReturn(Optional.of(fichaLivre()));
        when(mesaRepository.findByParticipantsCharacterId(10L)).thenReturn(List.of());
        when(solicitacaoRepository.existsByCharacterIdAndStatus(10L, StatusSolicitacao.PENDENTE)).thenReturn(true);

        assertThrows(RegraDeNegocioException.class, () -> service.aplicar(10L, 1L, "EXPLORADOR"));
    }

    @Test
    void deveLiberarPacoteQuandoAUnicaMesaDaFichaFoiEncerrada() {
        Personagem personagem = fichaLivre();
        when(personagemRepository.findById(10L)).thenReturn(Optional.of(personagem));
        when(mesaRepository.findByParticipantsCharacterId(10L))
                .thenReturn(new ArrayList<>(List.of(mesaComParticipante(StatusMesa.ENCERRADA))));
        when(solicitacaoRepository.existsByCharacterIdAndStatus(10L, StatusSolicitacao.PENDENTE)).thenReturn(false);
        when(personagemRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(pacoteRepository.findById("EXPLORADOR")).thenReturn(Optional.of(EXPLORADOR));

        assertEquals("EXPLORADOR", service.aplicar(10L, 1L, "EXPLORADOR").pacoteEquipamento());
    }

    @Test
    void deveNegarPacoteEmFichaDeOutroUsuario() {
        when(personagemRepository.findById(10L)).thenReturn(Optional.of(fichaLivre()));

        assertThrows(AccessDeniedException.class, () -> service.aplicar(10L, 2L, "EXPLORADOR"));
    }

    @Test
    void deveResponderNaoEncontradoParaPacoteInexistente() {
        prepararFichaLivre(fichaLivre());
        when(pacoteRepository.findById("VIAJANTE")).thenReturn(Optional.empty());

        assertThrows(RecursoNaoEncontradoException.class, () -> service.aplicar(10L, 1L, "viajante"));
    }
}
