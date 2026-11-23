package com.spring.catan.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.spring.catan.dto.RecursosDTO;
import com.spring.catan.service.JugadorService;

@RestController
@RequestMapping("/jugador")
public class JugadorController {
    private final JugadorService servei;

    public JugadorController(JugadorService servei) {
        this.servei = servei;
    }

    @GetMapping("/{idJugador}/recursos")
    public ResponseEntity<RecursosDTO> recursos(@PathVariable("idJugador") int idJugador) {
        RecursosDTO recursos = servei.recursos(idJugador);
        if (recursos == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(recursos);
    }
}
