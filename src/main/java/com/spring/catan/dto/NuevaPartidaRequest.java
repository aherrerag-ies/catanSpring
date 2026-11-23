package com.spring.catan.dto;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonAlias;

public class NuevaPartidaRequest {
    @JsonAlias({"jugadors", "nomsJugadors"})
    private List<String> jugadores;
    @JsonAlias("puntsPerGuanyar")
    private Integer puntosParaGanar;

    public List<String> getJugadores() {
        return jugadores;
    }

    public void setJugadores(List<String> jugadores) {
        this.jugadores = jugadores;
    }

    public Integer getPuntosParaGanar() {
        return puntosParaGanar;
    }

    public void setPuntosParaGanar(Integer puntosParaGanar) {
        this.puntosParaGanar = puntosParaGanar;
    }
}
