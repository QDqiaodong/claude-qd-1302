package com.aqua.farm.harvest;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HarvestRepository extends JpaRepository<Harvest, Long> {

    List<Harvest> findByPondIdOrderByHarvestDateDesc(Long pondId);

    List<Harvest> findByBuyer(String buyer);
}
