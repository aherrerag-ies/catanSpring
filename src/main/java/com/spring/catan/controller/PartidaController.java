package com.spring.catan.controller;

import java.util.List;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import com.spring.catan.dto.*;
import com.spring.catan.service.PartidaService;

@RestController
@RequestMapping("/partida")
public class PartidaController {
    private final PartidaService servei;

    public PartidaController(PartidaService servei) {
        this.servei = servei;
    }

    @GetMapping("/{idPartida}/tauler")
    public ResponseEntity<List<HexagonDTO>> tauler(@PathVariable("idPartida") int idPartida) {
        List<HexagonDTO> hexagons = servei.tauler(idPartida);
        if (hexagons == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(hexagons);
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody NuevaPartidaRequest peticio) {
        try {
            PartidaDTO partida = servei.crear(peticio);
            return ResponseEntity.status(HttpStatus.CREATED).body(partida);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{idPartida}/daus")
    public ResponseEntity<?> tirar(@PathVariable("idPartida") int idPartida) {
        try {
            TiradaDTO tirada = servei.tirarDaus(idPartida);
            if (tirada == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(tirada);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
