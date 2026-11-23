package com.spring.catan.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import relacions.Jugador;

public interface JugadorRepository extends JpaRepository<Jugador, Integer> {
    List<Jugador> findByPartida_IdOrderByOrdreTorn(int idPartida);
}
