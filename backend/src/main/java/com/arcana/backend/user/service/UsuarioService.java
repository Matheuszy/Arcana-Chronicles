package com.arcana.backend.user.service;

import com.arcana.backend.character.model.Personagem;
import com.arcana.backend.user.dto.request.UsuarioRequestDto;
import com.arcana.backend.user.model.Usuario;
import com.arcana.backend.user.repository.UsuarioRepositorie;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
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

    @Transactional
    public Optional<Usuario> findByPersonagemId(Long personagemId) {
        if (this.usuarioRepositorie.findByPersonagemId(personagemId).isPresent()) {
            return usuarioRepositorie.findByPersonagemId(personagemId);
        }
        else {
            throw new RuntimeException("Personagem não encontrado");
        }
    }

    @Transactional
    public Optional<Personagem> findByPersonagensIn(List<Personagem> personagens) {
        if (this.usuarioRepositorie.findByPersonagensIn(personagens).isPresent()) {
            return usuarioRepositorie.findByPersonagensIn(personagens);
        }
        else {
            throw new RuntimeException("Personagem não encontrado");
        }
    }

    @Transactional
    public Usuario update(UsuarioRequestDto novoUsuario) {
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

    @Transactional
    public void deleteById(Long id) {
        Optional<Usuario> usuarioExistente = usuarioRepositorie.findById(id);
        if (usuarioExistente.isPresent()) {
            usuarioRepositorie.delete(usuarioExistente.get());
        } else {
            throw new RuntimeException("Usuário não encontrado");
        }
    }
}
