package com.arcana.backend.table.controller;

import com.arcana.backend.character.PersonagemResponseDto;
import com.arcana.backend.table.MesaRequestDto;
import com.arcana.backend.table.MesaResponseDto;
import com.arcana.backend.table.SolicitacaoEntradaRequestDto;
import com.arcana.backend.table.SolicitacaoEntradaResponseDto;
import com.arcana.backend.table.model.StatusMesa;
import com.arcana.backend.table.service.MesaService;
import com.arcana.backend.user.model.Usuario;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
            @AuthenticationPrincipal Usuario usuario) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mesaService.criar(dto, usuario.getId(), usuario.getUsername(), usuario.getPapel()));
    }

    @PostMapping("/{id}/solicitacoes")
    public ResponseEntity<SolicitacaoEntradaResponseDto> solicitarEntrada(
            @PathVariable Long id,
            @RequestBody @Valid SolicitacaoEntradaRequestDto dto,
            @AuthenticationPrincipal Usuario usuario) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mesaService.solicitarEntrada(
                        id, usuario.getId(), usuario.getUsername(), usuario.getPapel(), dto.characterId()));
    }

    @GetMapping("/{id}/solicitacoes/minha")
    public ResponseEntity<SolicitacaoEntradaResponseDto> buscarMinhaSolicitacao(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario usuario) {

        return ResponseEntity.ok(mesaService.buscarMinhaSolicitacao(id, usuario.getId()));
    }

    @GetMapping("/{id}/solicitacoes")
    public ResponseEntity<List<SolicitacaoEntradaResponseDto>> listarSolicitacoesPendentes(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario usuario) {

        return ResponseEntity.ok(mesaService.listarSolicitacoesPendentes(id, usuario.getId()));
    }

    @PostMapping("/{id}/solicitacoes/{solicitacaoId}/aceitar")
    public ResponseEntity<SolicitacaoEntradaResponseDto> aceitarSolicitacao(
            @PathVariable Long id,
            @PathVariable Long solicitacaoId,
            @AuthenticationPrincipal Usuario usuario) {

        return ResponseEntity.ok(mesaService.aceitarSolicitacao(id, solicitacaoId, usuario.getId()));
    }

    @PostMapping("/{id}/solicitacoes/{solicitacaoId}/recusar")
    public ResponseEntity<SolicitacaoEntradaResponseDto> recusarSolicitacao(
            @PathVariable Long id,
            @PathVariable Long solicitacaoId,
            @AuthenticationPrincipal Usuario usuario) {

        return ResponseEntity.ok(mesaService.recusarSolicitacao(id, solicitacaoId, usuario.getId()));
    }

    @GetMapping("/{id}/fichas/{characterId}")
    public ResponseEntity<PersonagemResponseDto> buscarFichaDeParticipante(
            @PathVariable Long id,
            @PathVariable Long characterId,
            @AuthenticationPrincipal Usuario usuario) {

        return ResponseEntity.ok(mesaService.buscarFichaDeParticipante(id, characterId, usuario.getId()));
    }

    @PatchMapping("/{id}/personagem")
    public ResponseEntity<MesaResponseDto> trocarPersonagem(
            @PathVariable Long id,
            @RequestBody(required = false) TrocarPersonagemRequest body,
            @AuthenticationPrincipal Usuario usuario) {

        Long characterId = (body != null) ? body.characterId() : null;
        return ResponseEntity.ok(mesaService.trocarPersonagem(id, usuario.getId(), characterId));
    }

    /** POST /api/tables/{id}/sair */
    @PostMapping("/{id}/sair")
    public ResponseEntity<MesaResponseDto> sair(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario usuario) {

        return ResponseEntity.ok(mesaService.sair(id, usuario.getId()));
    }

    /**
     * PATCH /api/tables/{id}/status
     * Body: { "status": "EM_ANDAMENTO" }
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<MesaResponseDto> atualizarStatus(
            @PathVariable Long id,
            @RequestBody StatusRequest body,
            @AuthenticationPrincipal Usuario usuario) {

        return ResponseEntity.ok(mesaService.atualizarStatus(id, body.status(), usuario.getId()));
    }

    /** POST /api/tables/{id}/npcs/{npcId} */
    @PostMapping("/{id}/npcs/{npcId}")
    public ResponseEntity<MesaResponseDto> adicionarNpc(
            @PathVariable Long id,
            @PathVariable Long npcId,
            @AuthenticationPrincipal Usuario usuario) {

        return ResponseEntity.ok(mesaService.adicionarNpc(id, npcId, usuario.getId()));
    }

    /** DELETE /api/tables/{id}/npcs/{npcId} */
    @DeleteMapping("/{id}/npcs/{npcId}")
    public ResponseEntity<MesaResponseDto> removerNpc(
            @PathVariable Long id,
            @PathVariable Long npcId,
            @AuthenticationPrincipal Usuario usuario) {

        return ResponseEntity.ok(mesaService.removerNpc(id, npcId, usuario.getId()));
    }

    /** DELETE /api/tables/{id} */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario usuario) {

        mesaService.deletar(id, usuario.getId());
        return ResponseEntity.noContent().build();
    }

    // ── records de request body ──────────────────────────────────────────────
    record TrocarPersonagemRequest(Long characterId) {}
    record StatusRequest(StatusMesa status) {}
}

