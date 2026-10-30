package com.arcana.backend;

import com.arcana.backend.config.exception.RegraDeNegocioException;
import com.arcana.backend.config.security.TokenService;
import com.arcana.backend.table.SessoesAtivasDeMesa;
import com.arcana.backend.table.service.MesaService;
import com.arcana.backend.user.dto.request.AtualizarUsuarioRequestDto;
import com.arcana.backend.user.dto.request.LoginRequestDto;
import com.arcana.backend.user.dto.request.UsuarioRequestDto;
import com.arcana.backend.user.dto.response.LoginResponseDto;
import com.arcana.backend.user.dto.response.UsuarioResponseDto;
import com.arcana.backend.user.model.PapelUsuario;
import com.arcana.backend.user.model.Usuario;
import com.arcana.backend.user.repository.UsuarioRepositorie;
import com.arcana.backend.user.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepositorie repository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenService tokenService;

    @Mock
    private MesaService mesaService;

    @Mock
    private SessoesAtivasDeMesa sessoesAtivas;

    @InjectMocks
    private UsuarioService service;

    @Test
    void deveCadastrarUsuario() {
        UsuarioRequestDto dto = new UsuarioRequestDto("joao", "joao@gmail.com", "12334", PapelUsuario.JOGADOR);
        Usuario newUser = new Usuario("joao", "joao@gmail.com", "encodedPassword");

        when(passwordEncoder.encode("12334")).thenReturn("encodedPassword");
        when(repository.save(any(Usuario.class))).thenReturn(newUser);

        UsuarioResponseDto dtoReturn = service.criar(dto);

        assertNotNull(dtoReturn);
        assertEquals("joao@gmail.com", dtoReturn.email());

        verify(passwordEncoder).encode("12334");
        verify(repository).save(any(Usuario.class));
    }

    @Test
    void deveGravarPapelEscolhidoNoCadastro() {
        UsuarioRequestDto dto = new UsuarioRequestDto("gandalf", "gandalf@arcana.com", "123456", PapelUsuario.MESTRE);

        when(passwordEncoder.encode("123456")).thenReturn("encodedPassword");
        when(repository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UsuarioResponseDto dtoReturn = service.criar(dto);

        assertEquals(PapelUsuario.MESTRE, dtoReturn.papel());
    }

    @Test
    void deveLancarExcecaoQuandoUsernameJaExistir() {
        UsuarioRequestDto dto = new UsuarioRequestDto("theron", "joao@gmail.com", "12334", PapelUsuario.JOGADOR);
        Usuario existsUser = new Usuario("theron", "joao@gmail.com", "12334");

        when(repository.findByUsername(dto.username())).thenReturn(Optional.of(existsUser));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            service.criar(dto);
        });

        assertEquals("Username já está em uso: " + dto.username(), exception.getMessage());
        verify(repository, never()).save(any(Usuario.class));
    }

    @Test
    void deveAutenticarELogarUsuarioComTokenJwt() {
        LoginRequestDto loginDto = new LoginRequestDto("joao", "12334");
        Usuario user = new Usuario("joao", "joao@gmail.com", "encodedPassword", PapelUsuario.MESTRE);
        user.setId(1L);

        when(repository.findByUsername("joao")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("12334", "encodedPassword")).thenReturn(true);
        when(tokenService.gerarToken(user)).thenReturn("mocked-jwt-token");

        LoginResponseDto loginResponse = service.login(loginDto);

        assertNotNull(loginResponse);
        assertEquals("joao", loginResponse.username());
        assertEquals("mocked-jwt-token", loginResponse.token());
        assertEquals(1L, loginResponse.id());
        assertEquals(PapelUsuario.MESTRE, loginResponse.papel());
    }

    @Test
    void deveCompararSenhaMesmoQuandoUsuarioNaoExisteParaNaoRevelarContas() {
        when(repository.findByUsername("fantasma")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> service.login(new LoginRequestDto("fantasma", "qualquer")));

        assertEquals("Usuário ou senha inválidos.", exception.getMessage());
        verify(passwordEncoder).matches(eq("qualquer"), any());
    }

    @Test
    void deveLancarExcecaoQuandoSenhaInvalidaNoLogin() {
        LoginRequestDto loginDto = new LoginRequestDto("joao", "senha_errada");
        Usuario user = new Usuario("joao", "joao@gmail.com", "encodedPassword");

        when(repository.findByUsername("joao")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("senha_errada", "encodedPassword")).thenReturn(false);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            service.login(loginDto);
        });

        assertEquals("Usuário ou senha inválidos.", exception.getMessage());
    }

    @Test
    void devePermitirBuscarPorIdQuandoForOProprioUsuario() {
        Usuario user = new Usuario("joao", "joao@gmail.com", "encodedPassword");
        user.setId(1L);

        when(repository.findById(1L)).thenReturn(Optional.of(user));

        UsuarioResponseDto dto = service.buscarPorId(1L, 1L);

        assertEquals("joao", dto.username());
    }

    @Test
    void deveNegarBuscarPorIdDeOutroUsuario() {
        assertThrows(AccessDeniedException.class, () -> service.buscarPorId(1L, 2L));
        verify(repository, never()).findById(any());
    }

    @Test
    void deveNegarAtualizarDadosDeOutroUsuario() {
        AtualizarUsuarioRequestDto dto = new AtualizarUsuarioRequestDto("joao", "joao@gmail.com", "123456");

        assertThrows(AccessDeniedException.class, () -> service.atualizar(1L, dto, 2L));
        verify(repository, never()).findById(any());
    }

    private Usuario usuarioComId(Long id, String username, String email) {
        Usuario usuario = new Usuario(username, email, "hash-antigo", PapelUsuario.JOGADOR);
        usuario.setId(id);
        return usuario;
    }

    @Test
    void deveAtualizarNomeEEmailSemExigirNovaSenha() {
        clearInvocations(passwordEncoder);
        Usuario usuario = usuarioComId(1L, "joao", "joao@arcana.com");
        when(repository.findById(1L)).thenReturn(Optional.of(usuario));
        when(repository.findByUsername("joao2")).thenReturn(Optional.empty());
        when(repository.findByEmail("novo@arcana.com")).thenReturn(Optional.empty());
        when(repository.save(usuario)).thenReturn(usuario);

        when(tokenService.gerarToken(usuario)).thenReturn("token-novo");

        LoginResponseDto resposta = service.atualizar(1L,
                new AtualizarUsuarioRequestDto("joao2", "novo@arcana.com", null), 1L);

        assertEquals("joao2", resposta.username());
        assertEquals("novo@arcana.com", resposta.email());
        assertEquals("token-novo", resposta.token());
        assertEquals("hash-antigo", usuario.getPassword());
        assertEquals(PapelUsuario.JOGADOR, usuario.getPapel());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void deveTrocarASenhaQuandoUmaNovaForInformada() {
        Usuario usuario = usuarioComId(1L, "joao", "joao@arcana.com");
        when(repository.findById(1L)).thenReturn(Optional.of(usuario));
        when(repository.findByUsername("joao")).thenReturn(Optional.of(usuario));
        when(repository.findByEmail("joao@arcana.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.encode("segredo1")).thenReturn("hash-novo");
        when(repository.save(usuario)).thenReturn(usuario);

        service.atualizar(1L, new AtualizarUsuarioRequestDto("joao", "joao@arcana.com", "segredo1"), 1L);

        assertEquals("hash-novo", usuario.getPassword());
    }

    @Test
    void deveRecusarAtualizacaoComUsernameOuEmailDeOutraConta() {
        Usuario usuario = usuarioComId(1L, "joao", "joao@arcana.com");
        Usuario outro = usuarioComId(2L, "maria", "maria@arcana.com");
        when(repository.findById(1L)).thenReturn(Optional.of(usuario));
        when(repository.findByUsername("maria")).thenReturn(Optional.of(outro));
        when(repository.findByUsername("joao")).thenReturn(Optional.of(usuario));
        when(repository.findByEmail("maria@arcana.com")).thenReturn(Optional.of(outro));

        assertThrows(RegraDeNegocioException.class, () -> service.atualizar(1L,
                new AtualizarUsuarioRequestDto("maria", "joao@arcana.com", null), 1L));
        assertThrows(RegraDeNegocioException.class, () -> service.atualizar(1L,
                new AtualizarUsuarioRequestDto("joao", "maria@arcana.com", null), 1L));
        verify(repository, never()).save(any());
    }

    @Test
    void deveRecusarCadastroComEmailJaUsado() {
        when(repository.findByUsername("novo")).thenReturn(Optional.empty());
        when(repository.findByEmail("joao@arcana.com"))
                .thenReturn(Optional.of(usuarioComId(1L, "joao", "joao@arcana.com")));

        assertThrows(RegraDeNegocioException.class, () -> service.criar(
                new UsuarioRequestDto("novo", "joao@arcana.com", "123456", PapelUsuario.JOGADOR)));
        verify(repository, never()).save(any());
    }

    @Test
    void deveBloquearDadosDaContaEnquantoOUsuarioEstaEmUmaMesa() {
        when(sessoesAtivas.usuarioEmSessao(1L)).thenReturn(true);

        assertThrows(RegraDeNegocioException.class, () -> service.buscarPorId(1L, 1L));
        assertThrows(RegraDeNegocioException.class, () -> service.atualizar(1L,
                new AtualizarUsuarioRequestDto("joao", "joao@arcana.com", null), 1L));
        assertThrows(RegraDeNegocioException.class, () -> service.deletar(1L, 1L));
        verify(repository, never()).save(any());
        verify(mesaService, never()).removerVinculosDoUsuario(any());
    }

    @Test
    void deveLimparMesasEParticipacoesAntesDeExcluirAConta() {
        when(repository.existsById(1L)).thenReturn(true);

        service.deletar(1L, 1L);

        var ordem = inOrder(mesaService, repository);
        ordem.verify(mesaService).removerVinculosDoUsuario(1L);
        ordem.verify(repository).deleteById(1L);
    }

    @Test
    void deveNegarDeletarContaDeOutroUsuario() {
        assertThrows(AccessDeniedException.class, () -> service.deletar(1L, 2L));
        verify(repository, never()).deleteById(any());
    }
}

