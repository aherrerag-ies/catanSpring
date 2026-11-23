package com.spring.catan.dto;

import java.util.List;
import java.util.ArrayList;
import relacions.Jugador;
import relacions.RecursJugador;

public record RecursosDTO(Integer idJugador, String nom, List<RecursDTO> recursos) {
    public static RecursosDTO from(Jugador jugador) {
        List<RecursDTO> recursos = new ArrayList<>();
        for (RecursJugador recurs : jugador.getRecursos()) {
            recursos.add(new RecursDTO(recurs.getTipus().name(), recurs.getQuantitat()));
        }
        return new RecursosDTO(jugador.getId(), jugador.getNom(), recursos);
    }

    public record RecursDTO(String tipus, int quantitat) {
    }
}
