package com.arcana.backend.user.service;

import com.arcana.backend.config.exception.CredenciaisInvalidasException;
import com.arcana.backend.config.exception.RecursoNaoEncontradoException;
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
import jakarta.transaction.Transactional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UsuarioService {

    private static final int TENTATIVAS_DE_CODIGO_AMIGO = 10;

    private final UsuarioRepositorie usuarioRepositorie;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final MesaService mesaService;
    private final SessoesAtivasDeMesa sessoesAtivas;
    private final GeradorCodigoAmigo geradorCodigoAmigo;
    private final String hashParaComparacaoFalsa;

    public UsuarioService(UsuarioRepositorie usuarioRepositorie,
                          PasswordEncoder passwordEncoder,
                          TokenService tokenService,
                          MesaService mesaService,
                          SessoesAtivasDeMesa sessoesAtivas,
                          GeradorCodigoAmigo geradorCodigoAmigo) {
        this.usuarioRepositorie = usuarioRepositorie;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.mesaService = mesaService;
        this.sessoesAtivas = sessoesAtivas;
        this.geradorCodigoAmigo = geradorCodigoAmigo;
        this.hashParaComparacaoFalsa = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    /** Cadastra um novo usuário com senha criptografada em BCrypt */
    @Transactional
    public UsuarioResponseDto criar(UsuarioRequestDto dto) {
        if (usuarioRepositorie.findByUsernameIgnoreCase(dto.username()).isPresent()) {
            throw usuarioJaExiste(dto.username());
        }
        if (usuarioRepositorie.findByEmailIgnoreCaseAndPapel(dto.email(), dto.papel()).isPresent()) {
            throw emailJaUsadoNoPapel(dto.papel());
        }

        String encodedPassword = passwordEncoder.encode(dto.password());
        Usuario usuario = new Usuario(dto.username(), dto.email(), encodedPassword, dto.papel());
        usuario.setCodigoAmigo(gerarCodigoAmigoUnico());
        return UsuarioResponseDto.from(usuarioRepositorie.save(usuario));
    }

    /**
     * Autentica usuário e retorna dados com Token JWT.
     */
    public LoginResponseDto login(LoginRequestDto dto) {
        Usuario usuario = usuarioRepositorie.findByUsername(dto.username()).orElse(null);

        if (usuario == null) {
            passwordEncoder.matches(dto.password(), hashParaComparacaoFalsa);
            throw new CredenciaisInvalidasException("Usuário ou senha inválidos.");
        }

        if (!passwordEncoder.matches(dto.password(), usuario.getPassword())) {
            throw new CredenciaisInvalidasException("Usuário ou senha inválidos.");
        }

        String token = tokenService.gerarToken(usuario);
        return new LoginResponseDto(usuario.getId(), usuario.getUsername(), usuario.getEmail(), usuario.getPapel(), token);
    }

    /** Busca dados públicos do usuário por ID */
    public UsuarioResponseDto buscarPorId(Long id, Long usuarioAutenticadoId) {
        validarDonoDoRecurso(id, usuarioAutenticadoId);
        Usuario usuario = usuarioRepositorie.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado: " + id));
        return UsuarioResponseDto.from(usuario);
    }

    /** Atualiza username, email e senha */
    @Transactional
    public LoginResponseDto atualizar(Long id, AtualizarUsuarioRequestDto dto, Long usuarioAutenticadoId) {
        validarDonoDoRecurso(id, usuarioAutenticadoId);
        Usuario usuario = usuarioRepositorie.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado: " + id));

        usuarioRepositorie.findByUsernameIgnoreCase(dto.username())
                .filter(outro -> !outro.getId().equals(id))
                .ifPresent(outro -> {
                    throw usuarioJaExiste(dto.username());
                });
        usuarioRepositorie.findByEmailIgnoreCaseAndPapel(dto.email(), usuario.getPapel())
                .filter(outro -> !outro.getId().equals(id))
                .ifPresent(outro -> {
                    throw emailJaUsadoNoPapel(usuario.getPapel());
                });

        usuario.setUsername(dto.username());
        usuario.setEmail(dto.email());
        if (dto.password() != null && !dto.password().isBlank()) {
            usuario.setPassword(passwordEncoder.encode(dto.password()));
        }

        Usuario salvo = usuarioRepositorie.save(usuario);
        return new LoginResponseDto(salvo.getId(), salvo.getUsername(), salvo.getEmail(), salvo.getPapel(),
                tokenService.gerarToken(salvo));
    }

    /** Remove o usuário e todos os seus personagens (cascade) */
    @Transactional
    public void deletar(Long id, Long usuarioAutenticadoId) {
        validarDonoDoRecurso(id, usuarioAutenticadoId);
        if (!usuarioRepositorie.existsById(id)) {
            throw new RecursoNaoEncontradoException("Usuário não encontrado: " + id);
        }
        mesaService.removerVinculosDoUsuario(id);
        usuarioRepositorie.deleteById(id);
    }

    @Transactional
    public UsuarioResponseDto regenerarCodigoAmigo(Long id, Long usuarioAutenticadoId) {
        validarDonoDoRecurso(id, usuarioAutenticadoId);
        Usuario usuario = usuarioRepositorie.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado: " + id));

        usuario.setCodigoAmigo(gerarCodigoAmigoUnico());
        return UsuarioResponseDto.from(usuarioRepositorie.save(usuario));
    }

    private String gerarCodigoAmigoUnico() {
        for (int tentativa = 0; tentativa < TENTATIVAS_DE_CODIGO_AMIGO; tentativa++) {
            String codigo = geradorCodigoAmigo.gerar();
            if (!usuarioRepositorie.existsByCodigoAmigo(codigo)) {
                return codigo;
            }
        }
        throw new IllegalStateException("Não foi possível gerar um código de amigo único.");
    }

    private RegraDeNegocioException usuarioJaExiste(String username) {
        return new RegraDeNegocioException("O usuário \"" + username + "\" já existe. Escolha outro nome.");
    }

    private RegraDeNegocioException emailJaUsadoNoPapel(PapelUsuario papel) {
        String conta = papel == PapelUsuario.MESTRE ? "mestre" : "jogador";
        return new RegraDeNegocioException("Este email já tem uma conta de " + conta
                + ". Cada email pode ter uma conta de mestre e uma de jogador.");
    }

    private void validarDonoDoRecurso(Long id, Long usuarioAutenticadoId) {
        if (!id.equals(usuarioAutenticadoId)) {
            throw new AccessDeniedException("Usuário não pode acessar dados de outro usuário.");
        }
        if (sessoesAtivas.usuarioEmSessao(id)) {
            throw new RegraDeNegocioException("Você está em uma mesa. Saia da mesa para acessar os dados da conta.");
        }
    }
}

