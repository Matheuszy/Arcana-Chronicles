package com.arcana.backend.character.controller;

import com.arcana.backend.character.EquipamentoCatalogoDto;
import com.arcana.backend.character.model.CategoriaEquipamento;
import com.arcana.backend.character.service.EquipamentoCatalogoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/catalogo-equipamentos")
public class EquipamentoCatalogoController {

    private final EquipamentoCatalogoService equipamentoCatalogoService;

    public EquipamentoCatalogoController(EquipamentoCatalogoService equipamentoCatalogoService) {
        this.equipamentoCatalogoService = equipamentoCatalogoService;
    }

    @GetMapping
    public ResponseEntity<List<EquipamentoCatalogoDto>> sugerir(
            @RequestParam(defaultValue = "") String nome,
            @RequestParam(required = false) CategoriaEquipamento categoria) {
        return ResponseEntity.ok(equipamentoCatalogoService.sugerir(nome, categoria));
    }

    @GetMapping("/{chave}")
    public ResponseEntity<EquipamentoCatalogoDto> detalhar(@PathVariable String chave) {
        return ResponseEntity.ok(equipamentoCatalogoService.detalhar(chave));
    }
}
