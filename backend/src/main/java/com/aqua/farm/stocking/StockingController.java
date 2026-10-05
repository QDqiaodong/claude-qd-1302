package com.aqua.farm.stocking;

import com.aqua.farm.common.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stockings")
public class StockingController {

    private final StockingService service;

    public StockingController(StockingService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<Stocking>> list(@RequestParam(required = false) Long pondId) {
        return ApiResponse.success(service.list(pondId));
    }

    @PostMapping
    public ApiResponse<Stocking> create(@RequestBody Stocking in) {
        return ApiResponse.success(service.create(in));
    }

    @PutMapping("/{id}")
    public ApiResponse<Stocking> update(@PathVariable Long id, @RequestBody Stocking in) {
        return ApiResponse.success(service.update(id, in));
    }
}
