package com.spring.catan.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import taulell.HexagonVertex;

public interface HexagonVertexRepository extends JpaRepository<HexagonVertex, Integer> {
}
