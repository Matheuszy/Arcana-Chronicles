package com.arcana.backend.user.service;

import com.arcana.backend.user.dto.request.LoginRequestDto;
import com.arcana.backend.user.dto.request.UsuarioRequestDto;
import com.arcana.backend.user.dto.response.LoginResponseDto;
import com.arcana.backend.user.dto.response.UsuarioResponseDto;
import com.arcana.backend.user.model.Usuario;
import com.arcana.backend.user.repository.UsuarioRepositorie;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class UsuarioService {

    private final UsuarioRepositorie usuarioRepositorie;

    public UsuarioService(UsuarioRepositorie usuarioRepositorie) {
        this.usuarioRepositorie = usuarioRepositorie;
    }

    /** Cadastra um novo usuário */
    @Transactional
    public UsuarioResponseDto criar(UsuarioRequestDto dto) {
        if (usuarioRepositorie.findByUsername(dto.username()).isPresent()) {
            throw new RuntimeException("Username já está em uso: " + dto.username());
        }

        // TODO: quando Spring Security for adicionado, trocar por BCrypt.encode(dto.password())
        Usuario usuario = new Usuario(dto.username(), dto.email(), dto.password());
        return UsuarioResponseDto.from(usuarioRepositorie.save(usuario));
    }

    /**
     * Login simples por username + senha.
     * TODO: quando Spring Security for adicionado, gerar e retornar JWT aqui.
     */
    public LoginResponseDto login(LoginRequestDto dto) {
        Usuario usuario = usuarioRepositorie.findByUsername(dto.username())
                .orElseThrow(() -> new RuntimeException("Usuário ou senha inválidos."));

        // TODO: trocar por BCrypt.matches(dto.password(), usuario.getPassword())
        if (!usuario.getPassword().equals(dto.password())) {
            throw new RuntimeException("Usuário ou senha inválidos.");
        }

        return new LoginResponseDto(usuario.getId(), usuario.getUsername(), usuario.getEmail());
    }

    /** Busca dados públicos do usuário por ID */
    public UsuarioResponseDto buscarPorId(Long id) {
        Usuario usuario = usuarioRepositorie.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado: " + id));
        return UsuarioResponseDto.from(usuario);
    }

    /** Atualiza username e email */
    @Transactional
    public UsuarioResponseDto atualizar(Long id, UsuarioRequestDto dto) {
        Usuario usuario = usuarioRepositorie.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado: " + id));

        usuario.setUsername(dto.username());
        usuario.setEmail(dto.email());
        // TODO: encode senha antes de salvar
        usuario.setPassword(dto.password());

        return UsuarioResponseDto.from(usuarioRepositorie.save(usuario));
    }

    /** Remove o usuário e todos os seus personagens (cascade) */
    @Transactional
    public void deletar(Long id) {
        if (!usuarioRepositorie.existsById(id)) {
            throw new RuntimeException("Usuário não encontrado: " + id);
        }
        usuarioRepositorie.deleteById(id);
    }
}
