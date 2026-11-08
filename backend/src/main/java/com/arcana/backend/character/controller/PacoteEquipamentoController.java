package com.arcana.backend.character.controller;

import com.arcana.backend.character.PacoteEquipamentoDto;
import com.arcana.backend.character.service.PacoteEquipamentoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/pacotes-equipamento")
public class PacoteEquipamentoController {

    private final PacoteEquipamentoService pacoteEquipamentoService;

    public PacoteEquipamentoController(PacoteEquipamentoService pacoteEquipamentoService) {
        this.pacoteEquipamentoService = pacoteEquipamentoService;
    }

    @GetMapping
    public ResponseEntity<List<PacoteEquipamentoDto>> listar() {
        return ResponseEntity.ok(pacoteEquipamentoService.listar());
    }
}
