package com.arcana.backend.friend.controller;

import com.arcana.backend.friend.dto.request.BloquearUsuarioRequestDto;
import com.arcana.backend.friend.dto.request.EnviarSolicitacaoAmizadeRequestDto;
import com.arcana.backend.friend.dto.response.AmigoDto;
import com.arcana.backend.friend.dto.response.SolicitacaoAmizadeResponseDto;
import com.arcana.backend.friend.dto.response.UsuarioBloqueadoDto;
import com.arcana.backend.friend.dto.response.UsuarioEncontradoDto;
import com.arcana.backend.friend.service.AmizadeService;
import com.arcana.backend.user.model.Usuario;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/amigos")
public class AmizadeController {

    private final AmizadeService amizadeService;

    public AmizadeController(AmizadeService amizadeService) {
        this.amizadeService = amizadeService;
    }

    @GetMapping
    public ResponseEntity<List<AmigoDto>> listarAmigos(@AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(amizadeService.listarAmigos(usuario.getId()));
    }

    @DeleteMapping("/{amigoId}")
    public ResponseEntity<Void> removerAmizade(
            @PathVariable Long amigoId,
            @AuthenticationPrincipal Usuario usuario) {

        amizadeService.removerAmizade(usuario.getId(), amigoId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/bloqueios")
    public ResponseEntity<List<UsuarioBloqueadoDto>> listarBloqueados(@AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(amizadeService.listarBloqueados(usuario.getId()));
    }

    @PostMapping("/bloqueios")
    public ResponseEntity<UsuarioBloqueadoDto> bloquear(
            @RequestBody @Valid BloquearUsuarioRequestDto dto,
            @AuthenticationPrincipal Usuario usuario) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(amizadeService.bloquear(usuario.getId(), dto.usuarioId()));
    }

    @DeleteMapping("/bloqueios/{bloqueadoId}")
    public ResponseEntity<Void> desbloquear(
            @PathVariable Long bloqueadoId,
            @AuthenticationPrincipal Usuario usuario) {

        amizadeService.desbloquear(usuario.getId(), bloqueadoId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/busca")
    public ResponseEntity<List<UsuarioEncontradoDto>> buscar(
            @RequestParam(required = false) String termo,
            @AuthenticationPrincipal Usuario usuario) {

        return ResponseEntity.ok(amizadeService.buscar(usuario.getId(), termo));
    }

    @PostMapping("/solicitacoes")
    public ResponseEntity<SolicitacaoAmizadeResponseDto> enviar(
            @RequestBody @Valid EnviarSolicitacaoAmizadeRequestDto dto,
            @AuthenticationPrincipal Usuario usuario) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(amizadeService.enviar(usuario.getId(), dto.destinatarioId()));
    }

    @GetMapping("/solicitacoes/recebidas")
    public ResponseEntity<List<SolicitacaoAmizadeResponseDto>> listarRecebidas(
            @AuthenticationPrincipal Usuario usuario) {

        return ResponseEntity.ok(amizadeService.listarRecebidas(usuario.getId()));
    }

    @GetMapping("/solicitacoes/enviadas")
    public ResponseEntity<List<SolicitacaoAmizadeResponseDto>> listarEnviadas(
            @AuthenticationPrincipal Usuario usuario) {

        return ResponseEntity.ok(amizadeService.listarEnviadas(usuario.getId()));
    }

    @PostMapping("/solicitacoes/{id}/aceitar")
    public ResponseEntity<SolicitacaoAmizadeResponseDto> aceitar(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario usuario) {

        return ResponseEntity.ok(amizadeService.aceitar(id, usuario.getId()));
    }

    @PostMapping("/solicitacoes/{id}/recusar")
    public ResponseEntity<SolicitacaoAmizadeResponseDto> recusar(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario usuario) {

        return ResponseEntity.ok(amizadeService.recusar(id, usuario.getId()));
    }

    @PostMapping("/solicitacoes/{id}/cancelar")
    public ResponseEntity<SolicitacaoAmizadeResponseDto> cancelar(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario usuario) {

        return ResponseEntity.ok(amizadeService.cancelar(id, usuario.getId()));
    }
}
