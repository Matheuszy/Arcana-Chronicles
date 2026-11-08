package com.arcana.backend.user.service;

import com.arcana.backend.config.exception.RecursoNaoEncontradoException;
import com.arcana.backend.user.dto.response.NotaVistaResponseDto;
import com.arcana.backend.user.model.Usuario;
import com.arcana.backend.user.repository.UsuarioRepositorie;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class NotasDeAtualizacaoService {

    private final UsuarioRepositorie usuarioRepositorie;

    public NotasDeAtualizacaoService(UsuarioRepositorie usuarioRepositorie) {
        this.usuarioRepositorie = usuarioRepositorie;
    }

    public NotaVistaResponseDto ultimaVista(Long usuarioId) {
        return new NotaVistaResponseDto(buscar(usuarioId).getUltimaNotaVista());
    }

    @Transactional
    public NotaVistaResponseDto registrarVista(Long usuarioId, String versao) {
        Usuario usuario = buscar(usuarioId);
        usuario.setUltimaNotaVista(versao);
        usuarioRepositorie.save(usuario);
        return new NotaVistaResponseDto(versao);
    }

    private Usuario buscar(Long usuarioId) {
        return usuarioRepositorie.findById(usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado: " + usuarioId));
    }
}
