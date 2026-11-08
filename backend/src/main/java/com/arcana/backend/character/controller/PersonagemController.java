package com.arcana.backend.character.controller;

import com.arcana.backend.character.AcrescimoDeMoedasRequestDto;
import com.arcana.backend.character.AplicarPacoteRequestDto;
import com.arcana.backend.character.CaAdicionalRequestDto;
import com.arcana.backend.character.GastoDeDadosDeVidaRequestDto;
import com.arcana.backend.character.PersonagemRequestDto;
import com.arcana.backend.character.PersonagemResponseDto;
import com.arcana.backend.character.ResultadoRolagemDeFichaDto;
import com.arcana.backend.character.RolagemDeFichaDto;
import com.arcana.backend.character.RolagemDeMoedasRequestDto;
import com.arcana.backend.character.RolagemDeVidaRequestDto;
import com.arcana.backend.character.model.TipoJogador;
import com.arcana.backend.character.secao.SecaoAtributosDto;
import com.arcana.backend.character.secao.SecaoAtributosRequestDto;
import com.arcana.backend.character.secao.SecaoEquipamentoDto;
import com.arcana.backend.character.secao.SecaoEquipamentoRequestDto;
import com.arcana.backend.character.secao.SecaoHabilidadesDto;
import com.arcana.backend.character.secao.SecaoHabilidadesRequestDto;
import com.arcana.backend.character.secao.SecaoMagiasDto;
import com.arcana.backend.character.secao.SecaoMagiasRequestDto;
import com.arcana.backend.character.secao.SecaoPersonagemDto;
import com.arcana.backend.character.secao.SecaoPersonagemRequestDto;
import com.arcana.backend.character.secao.SecaoRolagensDto;
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

    @GetMapping("/{id}/ficha/personagem")
    public ResponseEntity<SecaoPersonagemDto> buscarSecaoPersonagem(
            @PathVariable Long id, @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(personagemService.buscarSecaoPersonagem(id, usuario.getId()));
    }

    @PutMapping("/{id}/ficha/personagem")
    public ResponseEntity<SecaoPersonagemDto> atualizarSecaoPersonagem(
            @PathVariable Long id,
            @Valid @RequestBody SecaoPersonagemRequestDto dto,
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(personagemService.atualizarSecaoPersonagem(id, dto, usuario.getId()));
    }

    @GetMapping("/{id}/ficha/atributos")
    public ResponseEntity<SecaoAtributosDto> buscarSecaoAtributos(
            @PathVariable Long id, @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(personagemService.buscarSecaoAtributos(id, usuario.getId()));
    }

    @PutMapping("/{id}/ficha/atributos")
    public ResponseEntity<SecaoAtributosDto> atualizarSecaoAtributos(
            @PathVariable Long id,
            @Valid @RequestBody SecaoAtributosRequestDto dto,
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(personagemService.atualizarSecaoAtributos(id, dto, usuario.getId()));
    }

    @GetMapping("/{id}/ficha/habilidades")
    public ResponseEntity<SecaoHabilidadesDto> buscarSecaoHabilidades(
            @PathVariable Long id, @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(personagemService.buscarSecaoHabilidades(id, usuario.getId()));
    }

    @PutMapping("/{id}/ficha/habilidades")
    public ResponseEntity<SecaoHabilidadesDto> atualizarSecaoHabilidades(
            @PathVariable Long id,
            @Valid @RequestBody SecaoHabilidadesRequestDto dto,
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(personagemService.atualizarSecaoHabilidades(id, dto, usuario.getId()));
    }

    @GetMapping("/{id}/ficha/equipamento")
    public ResponseEntity<SecaoEquipamentoDto> buscarSecaoEquipamento(
            @PathVariable Long id, @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(personagemService.buscarSecaoEquipamento(id, usuario.getId()));
    }

    @PutMapping("/{id}/ficha/equipamento")
    public ResponseEntity<SecaoEquipamentoDto> atualizarSecaoEquipamento(
            @PathVariable Long id,
            @Valid @RequestBody SecaoEquipamentoRequestDto dto,
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(personagemService.atualizarSecaoEquipamento(id, dto, usuario.getId()));
    }

    @GetMapping("/{id}/ficha/magias")
    public ResponseEntity<SecaoMagiasDto> buscarSecaoMagias(
            @PathVariable Long id, @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(personagemService.buscarSecaoMagias(id, usuario.getId()));
    }

    @PutMapping("/{id}/ficha/magias")
    public ResponseEntity<SecaoMagiasDto> atualizarSecaoMagias(
            @PathVariable Long id,
            @Valid @RequestBody SecaoMagiasRequestDto dto,
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(personagemService.atualizarSecaoMagias(id, dto, usuario.getId()));
    }

    @GetMapping("/{id}/ficha/rolagens")
    public ResponseEntity<SecaoRolagensDto> buscarSecaoRolagens(
            @PathVariable Long id, @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(personagemService.buscarSecaoRolagens(id, usuario.getId()));
    }

    @PostMapping("/{id}/moedas")
    public ResponseEntity<PersonagemResponseDto> acrescentarMoedas(
            @PathVariable Long id,
            @Valid @RequestBody AcrescimoDeMoedasRequestDto pedido,
            @AuthenticationPrincipal Usuario usuario) {

        return ResponseEntity.ok(personagemService.acrescentarMoedas(id, usuario.getId(), pedido.moeda(),
                pedido.quantidade()));
    }

    @PatchMapping("/{id}/ca-adicional")
    public ResponseEntity<PersonagemResponseDto> definirCaAdicional(
            @PathVariable Long id,
            @Valid @RequestBody CaAdicionalRequestDto pedido,
            @AuthenticationPrincipal Usuario usuario) {

        return ResponseEntity.ok(personagemService.definirCaAdicional(id, usuario.getId(), pedido.valor()));
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

