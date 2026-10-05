package com.aqua.farm.water;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WaterRepository extends JpaRepository<Water, Long> {

    List<Water> findByPondIdOrderByCheckDateDesc(Long pondId);

    List<Water> findByStatus(String status);
}
