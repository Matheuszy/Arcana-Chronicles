package com.arcana.backend.user.controller;

import com.arcana.backend.user.dto.request.NotaVistaRequestDto;
import com.arcana.backend.user.dto.response.NotaVistaResponseDto;
import com.arcana.backend.user.model.Usuario;
import com.arcana.backend.user.service.NotasDeAtualizacaoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notas-de-atualizacao")
public class NotasDeAtualizacaoController {

    private final NotasDeAtualizacaoService notasService;

    public NotasDeAtualizacaoController(NotasDeAtualizacaoService notasService) {
        this.notasService = notasService;
    }

    @GetMapping("/vista")
    public ResponseEntity<NotaVistaResponseDto> ultimaVista(@AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(notasService.ultimaVista(usuario.getId()));
    }

    @PutMapping("/vista")
    public ResponseEntity<NotaVistaResponseDto> registrarVista(
            @Valid @RequestBody NotaVistaRequestDto pedido,
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(notasService.registrarVista(usuario.getId(), pedido.versao()));
    }
}
