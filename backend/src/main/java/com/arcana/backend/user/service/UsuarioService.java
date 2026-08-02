package com.arcana.backend.user.service;

import com.arcana.backend.user.model.Usuario;
import com.arcana.backend.user.repository.UsuarioRepositorie;
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
}
