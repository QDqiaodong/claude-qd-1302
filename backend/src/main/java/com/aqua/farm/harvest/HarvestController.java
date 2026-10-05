package com.aqua.farm.harvest;

import com.aqua.farm.common.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/harvests")
public class HarvestController {

    private final HarvestService service;

    public HarvestController(HarvestService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<Harvest>> list(@RequestParam(required = false) Long pondId) {
        return ApiResponse.success(service.list(pondId));
    }

    @PostMapping
    public ApiResponse<Harvest> create(@RequestBody Harvest in) {
        return ApiResponse.success(service.create(in));
    }

    @PutMapping("/{id}")
    public ApiResponse<Harvest> update(@PathVariable Long id, @RequestBody Harvest in) {
        return ApiResponse.success(service.update(id, in));
    }
}
