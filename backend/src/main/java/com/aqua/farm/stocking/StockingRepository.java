package com.aqua.farm.stocking;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockingRepository extends JpaRepository<Stocking, Long> {

    List<Stocking> findByPondIdOrderByReleaseDateDesc(Long pondId);

    List<Stocking> findByFryType(String fryType);
}
