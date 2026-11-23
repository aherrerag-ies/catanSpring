package com.spring.catan.repository;

import java.util.List;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import herencia.Assentament;

public interface AssentamentRepository extends JpaRepository<Assentament, Integer> {
    @Query("""
            select a from Assentament a
            where a.vertex.id in (select hv.vertex.id from HexagonVertex hv where hv.hexagon.id = :id)
            order by a.vertex.codi
            """)
    List<Assentament> findAdjacents(@Param("id") int idHexagon);
}
