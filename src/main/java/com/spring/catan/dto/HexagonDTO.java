package com.spring.catan.dto;

import java.util.List;

public record HexagonDTO(Integer id, int codi, String terreny, Integer numero, boolean lladre,
                         List<ConstruccioDTO> construccions) {
    public record ConstruccioDTO(Integer id, String tipus, int idVertex,
                                 Integer idJugador, String nomJugador) {
    }
}
