package com.arcana.backend.user.service;

import com.arcana.backend.config.exception.CredenciaisInvalidasException;
import com.arcana.backend.config.exception.RecursoNaoEncontradoException;
import com.arcana.backend.config.exception.RegraDeNegocioException;
import com.arcana.backend.config.security.TokenService;
import com.arcana.backend.user.dto.request.LoginRequestDto;
import com.arcana.backend.user.dto.request.UsuarioRequestDto;
import com.arcana.backend.user.dto.response.LoginResponseDto;
import com.arcana.backend.user.dto.response.UsuarioResponseDto;
import com.arcana.backend.user.model.Usuario;
import com.arcana.backend.user.repository.UsuarioRepositorie;
import jakarta.transaction.Transactional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UsuarioService {

    private final UsuarioRepositorie usuarioRepositorie;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public UsuarioService(UsuarioRepositorie usuarioRepositorie,
                          PasswordEncoder passwordEncoder,
                          TokenService tokenService) {
        this.usuarioRepositorie = usuarioRepositorie;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    /** Cadastra um novo usuário com senha criptografada em BCrypt */
    @Transactional
    public UsuarioResponseDto criar(UsuarioRequestDto dto) {
        if (usuarioRepositorie.findByUsername(dto.username()).isPresent()) {
            throw new RegraDeNegocioException("Username já está em uso: " + dto.username());
        }

        String encodedPassword = passwordEncoder.encode(dto.password());
        Usuario usuario = new Usuario(dto.username(), dto.email(), encodedPassword);
        return UsuarioResponseDto.from(usuarioRepositorie.save(usuario));
    }

    /**
     * Autentica usuário e retorna dados com Token JWT.
     */
    public LoginResponseDto login(LoginRequestDto dto) {
        Usuario usuario = usuarioRepositorie.findByUsername(dto.username())
                .orElseThrow(() -> new CredenciaisInvalidasException("Usuário ou senha inválidos."));

        if (!passwordEncoder.matches(dto.password(), usuario.getPassword())) {
            throw new CredenciaisInvalidasException("Usuário ou senha inválidos.");
        }

        String token = tokenService.gerarToken(usuario);
        return new LoginResponseDto(usuario.getId(), usuario.getUsername(), usuario.getEmail(), token);
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
    public UsuarioResponseDto atualizar(Long id, UsuarioRequestDto dto, Long usuarioAutenticadoId) {
        validarDonoDoRecurso(id, usuarioAutenticadoId);
        Usuario usuario = usuarioRepositorie.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado: " + id));

        usuario.setUsername(dto.username());
        usuario.setEmail(dto.email());
        if (dto.password() != null && !dto.password().isBlank()) {
            usuario.setPassword(passwordEncoder.encode(dto.password()));
        }

        return UsuarioResponseDto.from(usuarioRepositorie.save(usuario));
    }

    /** Remove o usuário e todos os seus personagens (cascade) */
    @Transactional
    public void deletar(Long id, Long usuarioAutenticadoId) {
        validarDonoDoRecurso(id, usuarioAutenticadoId);
        if (!usuarioRepositorie.existsById(id)) {
            throw new RecursoNaoEncontradoException("Usuário não encontrado: " + id);
        }
        usuarioRepositorie.deleteById(id);
    }

    private void validarDonoDoRecurso(Long id, Long usuarioAutenticadoId) {
        if (!id.equals(usuarioAutenticadoId)) {
            throw new AccessDeniedException("Usuário não pode acessar dados de outro usuário.");
        }
    }
}

