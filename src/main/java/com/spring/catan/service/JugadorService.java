package com.spring.catan.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.spring.catan.dto.RecursosDTO;
import com.spring.catan.repository.JugadorRepository;
import relacions.Jugador;

@Service
@Transactional(readOnly = true)
public class JugadorService {
    private final JugadorRepository jugadors;

    public JugadorService(JugadorRepository jugadors) {
        this.jugadors = jugadors;
    }

    public RecursosDTO recursos(int idJugador) {
        Jugador jugador = jugadors.findById(idJugador).orElse(null);
        if (jugador == null) {
            return null;
        }
        return RecursosDTO.from(jugador);
    }
}
