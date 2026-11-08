package com.arcana.backend.character.controller;

import com.arcana.backend.character.AplicarPacoteRequestDto;
import com.arcana.backend.character.GastoDeDadosDeVidaRequestDto;
import com.arcana.backend.character.PersonagemRequestDto;
import com.arcana.backend.character.PersonagemResponseDto;
import com.arcana.backend.character.ResultadoRolagemDeFichaDto;
import com.arcana.backend.character.RolagemDeFichaDto;
import com.arcana.backend.character.RolagemDeMoedasRequestDto;
import com.arcana.backend.character.RolagemDeVidaRequestDto;
import com.arcana.backend.character.model.TipoJogador;
import com.arcana.backend.character.service.PacoteEquipamentoService;
import com.arcana.backend.character.service.PersonagemService;
import com.arcana.backend.character.service.RolagemDeFichaService;
import com.arcana.backend.user.model.Usuario;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/characters")
public class PersonagemController {

    private final PersonagemService personagemService;
    private final RolagemDeFichaService rolagemDeFichaService;
    private final PacoteEquipamentoService pacoteEquipamentoService;

    public PersonagemController(PersonagemService personagemService, RolagemDeFichaService rolagemDeFichaService,
                                PacoteEquipamentoService pacoteEquipamentoService) {
        this.personagemService = personagemService;
        this.rolagemDeFichaService = rolagemDeFichaService;
        this.pacoteEquipamentoService = pacoteEquipamentoService;
    }

    @PutMapping("/{id}/pacote-equipamento")
    public ResponseEntity<PersonagemResponseDto> aplicarPacote(
            @PathVariable Long id,
            @Valid @RequestBody AplicarPacoteRequestDto pedido,
            @AuthenticationPrincipal Usuario usuario) {

        return ResponseEntity.ok(pacoteEquipamentoService.aplicar(id, usuario.getId(), pedido.pacote()));
    }

    @GetMapping("/{id}/rolagens")
    public ResponseEntity<List<RolagemDeFichaDto>> listarRolagens(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario usuario) {

        return ResponseEntity.ok(rolagemDeFichaService.listar(id, usuario.getId()));
    }

    @PostMapping("/{id}/rolagens/vida")
    public ResponseEntity<ResultadoRolagemDeFichaDto> rolarVida(
            @PathVariable Long id,
            @Valid @RequestBody RolagemDeVidaRequestDto pedido,
            @AuthenticationPrincipal Usuario usuario) {

        return ResponseEntity.ok(rolagemDeFichaService.rolarVida(id, usuario.getId(), pedido));
    }

    @PostMapping("/{id}/rolagens/moedas")
    public ResponseEntity<ResultadoRolagemDeFichaDto> rolarMoedas(
            @PathVariable Long id,
            @Valid @RequestBody RolagemDeMoedasRequestDto pedido,
            @AuthenticationPrincipal Usuario usuario) {

        return ResponseEntity.ok(rolagemDeFichaService.rolarMoedas(id, usuario.getId(), pedido));
    }

    @PostMapping("/{id}/rolagens/dados-de-vida")
    public ResponseEntity<ResultadoRolagemDeFichaDto> gastarDadosDeVida(
            @PathVariable Long id,
            @Valid @RequestBody GastoDeDadosDeVidaRequestDto pedido,
            @AuthenticationPrincipal Usuario usuario) {

        return ResponseEntity.ok(rolagemDeFichaService.gastarDadosDeVida(id, usuario.getId(), pedido));
    }

    /** GET /api/characters?kind=PLAYER */
    @GetMapping
    public ResponseEntity<List<PersonagemResponseDto>> listar(
            @AuthenticationPrincipal Usuario usuario,
            @RequestParam(required = false) TipoJogador kind) {

        return ResponseEntity.ok(personagemService.listarPorUsuario(usuario.getId(), kind));
    }

    /** GET /api/characters/{id} */
    @GetMapping("/{id}")
    public ResponseEntity<PersonagemResponseDto> buscarPorId(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario usuario) {

        return ResponseEntity.ok(personagemService.buscarPorId(id, usuario.getId()));
    }

    /** POST /api/characters */
    @PostMapping
    public ResponseEntity<PersonagemResponseDto> criar(
            @RequestBody @Valid PersonagemRequestDto dto,
            @AuthenticationPrincipal Usuario usuario) {

        PersonagemResponseDto criado = personagemService.criar(dto, usuario.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(criado);
    }

    /** PUT /api/characters/{id} */
    @PutMapping("/{id}")
    public ResponseEntity<PersonagemResponseDto> atualizar(
            @PathVariable Long id,
            @RequestBody @Valid PersonagemRequestDto dto,
            @AuthenticationPrincipal Usuario usuario) {

        return ResponseEntity.ok(personagemService.atualizar(id, dto, usuario.getId()));
    }

    /** DELETE /api/characters/{id} */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario usuario) {

        personagemService.deletar(id, usuario.getId());
        return ResponseEntity.noContent().build();
    }
}

