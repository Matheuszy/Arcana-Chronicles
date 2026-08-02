package com.arcana.backend.user.service;

import com.arcana.backend.user.dto.request.UsuarioRequestDto;
import com.arcana.backend.user.model.Usuario;
import com.arcana.backend.user.repository.UsuarioRepositorie;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UsuarioService {

    private UsuarioRepositorie usuarioRepositorie;

    public UsuarioService(UsuarioRepositorie usuarioRepositorie) {
        this.usuarioRepositorie = usuarioRepositorie;
    }

    public Optional<Usuario> findByUsername(String username) {
        if (this.usuarioRepositorie.findByUsername(username).isPresent()) {
            return usuarioRepositorie.findByUsername(username);
        }
        else {
            throw new RuntimeException("Usuário não encontrado");
        }
    }

    public Optional<Usuario> findByEmail(String email) {
        if (this.usuarioRepositorie.findByEmail(email).isPresent()) {
            return usuarioRepositorie.findByEmail(email);
        }
        else {
            throw new RuntimeException("Email não encontrado");
        }
    }

    @Transactional
    public Optional<Usuario> findByPersonagemId(Long personagemId) {
        if (this.usuarioRepositorie.findeByPersonagemId(personagemId).isPresent()) {
            return usuarioRepositorie.findeByPersonagemId(personagemId);
        }
        else {
            throw new RuntimeException("Personagem não encontrado");
        }
    }

    @Transactional
    public Usuario updateByUsername(UsuarioRequestDto novoUsuario) {
        Optional<Usuario> usuarioExistente = usuarioRepositorie.findByUsername(novoUsuario.username());
        if (usuarioExistente.isPresent()) {
            Usuario usuario = usuarioExistente.get();
            usuario.setUsername(novoUsuario.username());
            usuario.setEmail(novoUsuario.email());
            usuario.setPassword(novoUsuario.password());
            return usuarioRepositorie.save(usuario);
        } else {
            throw new RuntimeException("Usuário não encontrado");
        }
    }
}
