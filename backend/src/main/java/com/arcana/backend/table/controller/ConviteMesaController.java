package com.arcana.backend.table.controller;

import com.arcana.backend.table.AceitarConviteRequestDto;
import com.arcana.backend.table.AceiteDeConviteDto;
import com.arcana.backend.table.ConvidarAmigoRequestDto;
import com.arcana.backend.table.ConviteMesaDto;
import com.arcana.backend.table.service.ConviteMesaService;
import com.arcana.backend.user.model.Usuario;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ConviteMesaController {

    private final ConviteMesaService conviteService;

    public ConviteMesaController(ConviteMesaService conviteService) {
        this.conviteService = conviteService;
    }

    @PostMapping("/tables/{mesaId}/convites")
    public ResponseEntity<ConviteMesaDto> convidar(
            @PathVariable Long mesaId,
            @RequestBody @Valid ConvidarAmigoRequestDto dto,
            @AuthenticationPrincipal Usuario usuario) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(conviteService.convidar(mesaId, usuario.getId(), dto.convidadoId()));
    }

    @GetMapping("/convites")
    public ResponseEntity<List<ConviteMesaDto>> listarPendentes(@AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(conviteService.listarPendentes(usuario.getId()));
    }

    @PostMapping("/convites/{id}/aceitar")
    public ResponseEntity<AceiteDeConviteDto> aceitar(
            @PathVariable Long id,
            @RequestBody @Valid AceitarConviteRequestDto dto,
            @AuthenticationPrincipal Usuario usuario) {

        return ResponseEntity.ok(conviteService.aceitar(id, usuario.getId(), dto.characterId()));
    }

    @PostMapping("/convites/{id}/recusar")
    public ResponseEntity<ConviteMesaDto> recusar(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario usuario) {

        return ResponseEntity.ok(conviteService.recusar(id, usuario.getId()));
    }
}
