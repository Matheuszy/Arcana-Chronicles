package com.arcana.backend.table.controller;

import com.arcana.backend.table.MesaRequestDto;
import com.arcana.backend.table.MesaResponseDto;
import com.arcana.backend.table.model.StatusMesa;
import com.arcana.backend.table.service.MesaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints consumidos pelo frontend em /api/tables
 *
 * X-Owner-Id: ID do usuário logado (temporário até ter JWT).
 * X-Display-Name: nome de exibição do usuário (usado ao entrar na mesa).
 */
@RestController
@RequestMapping("/api/tables")
public class MesaController {

    private final MesaService mesaService;

    public MesaController(MesaService mesaService) {
        this.mesaService = mesaService;
    }

    /** GET /api/tables */
    @GetMapping
    public ResponseEntity<List<MesaResponseDto>> listar() {
        return ResponseEntity.ok(mesaService.listarTodas());
    }

    /** GET /api/tables/{id} */
    @GetMapping("/{id}")
    public ResponseEntity<MesaResponseDto> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(mesaService.buscarPorId(id));
    }

    /** POST /api/tables */
    @PostMapping
    public ResponseEntity<MesaResponseDto> criar(
            @RequestBody @Valid MesaRequestDto dto,
            @RequestHeader("X-Owner-Id") Long masterId) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mesaService.criar(dto, masterId));
    }

    /**
     * POST /api/tables/{id}/entrar
     * Body: { "characterId": 42 }  (opcional)
     */
    @PostMapping("/{id}/entrar")
    public ResponseEntity<MesaResponseDto> entrar(
            @PathVariable Long id,
            @RequestBody(required = false) EntrarRequest body,
            @RequestHeader("X-Owner-Id") Long userId,
            @RequestHeader("X-Display-Name") String displayName) {

        Long characterId = (body != null) ? body.characterId() : null;
        return ResponseEntity.ok(mesaService.entrar(id, userId, displayName, characterId));
    }

    /**
     * PATCH /api/tables/{id}/status
     * Body: { "status": "EM_ANDAMENTO" }
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<MesaResponseDto> atualizarStatus(
            @PathVariable Long id,
            @RequestBody StatusRequest body,
            @RequestHeader("X-Owner-Id") Long masterId) {

        return ResponseEntity.ok(mesaService.atualizarStatus(id, body.status(), masterId));
    }

    /** DELETE /api/tables/{id} */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(
            @PathVariable Long id,
            @RequestHeader("X-Owner-Id") Long masterId) {

        mesaService.deletar(id, masterId);
        return ResponseEntity.noContent().build();
    }

    // ── records de request body ──────────────────────────────────────────────
    record EntrarRequest(Long characterId) {}
    record StatusRequest(StatusMesa status) {}
}
