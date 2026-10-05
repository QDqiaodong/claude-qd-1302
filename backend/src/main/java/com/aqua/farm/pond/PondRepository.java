package com.aqua.farm.pond;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PondRepository extends JpaRepository<Pond, Long> {

    List<Pond> findByStatus(String status);

    boolean existsByPondNo(String pondNo);
}
