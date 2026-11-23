package com.arcana.backend.character.controller;

import com.arcana.backend.character.MagiaCatalogoDto;
import com.arcana.backend.character.MagiaCatalogoResumoDto;
import com.arcana.backend.character.service.MagiaCatalogoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/catalogo-magias")
public class MagiaCatalogoController {

    private final MagiaCatalogoService magiaCatalogoService;

    public MagiaCatalogoController(MagiaCatalogoService magiaCatalogoService) {
        this.magiaCatalogoService = magiaCatalogoService;
    }

    @GetMapping
    public ResponseEntity<List<MagiaCatalogoResumoDto>> sugerir(
            @RequestParam(defaultValue = "") String nome,
            @RequestParam(required = false) Integer nivel,
            @RequestParam(required = false) Boolean truques,
            @RequestParam(required = false) List<String> classes) {
        return ResponseEntity.ok(magiaCatalogoService.sugerir(nome, nivel, truques, classes));
    }

    @GetMapping("/{chave}")
    public ResponseEntity<MagiaCatalogoDto> detalhar(@PathVariable String chave) {
        return ResponseEntity.ok(magiaCatalogoService.detalhar(chave));
    }
}
