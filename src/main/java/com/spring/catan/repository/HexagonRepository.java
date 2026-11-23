package com.spring.catan.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import taulell.Hexagon;

public interface HexagonRepository extends JpaRepository<Hexagon, Integer> {
    List<Hexagon> findByPartida_IdOrderByCodi(int idPartida);
    List<Hexagon> findByPartida_IdAndNumeroOrderByCodi(int idPartida, int numero);
}
