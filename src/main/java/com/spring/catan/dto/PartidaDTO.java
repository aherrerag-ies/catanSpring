package com.spring.catan.dto;

import java.util.List;
import relacions.Jugador;

public record PartidaDTO(Integer idPartida, String estat, int ronda, int puntsPerGuanyar,
                         Integer idJugadorActiu, List<JugadorDTO> jugadors, List<HexagonDTO> hexagons) {
    public record JugadorDTO(Integer idJugador, String nom, String color, int ordreTorn,
                             List<RecursosDTO.RecursDTO> recursos) {
        public static JugadorDTO from(Jugador jugador) {
            return new JugadorDTO(jugador.getId(), jugador.getNom(), jugador.getColor().name(),
                    jugador.getOrdreTorn(), RecursosDTO.from(jugador).recursos());
        }
    }
}
