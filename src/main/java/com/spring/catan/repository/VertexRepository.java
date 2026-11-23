package com.spring.catan.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import taulell.Vertex;

public interface VertexRepository extends JpaRepository<Vertex, Integer> {
}
