package com.spring.catan.repository;

import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import relacions.Partida;

public interface PartidaRepository extends JpaRepository<Partida, Integer> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Partida p where p.id = :id")
    Optional<Partida> findForUpdate(@Param("id") int id);
}
