package com.aqua.farm.pond;

import com.aqua.farm.common.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ponds")
public class PondController {

    private final PondService service;

    public PondController(PondService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<Pond>> list(@RequestParam(required = false) String status) {
        return ApiResponse.success(service.list(status));
    }

    @GetMapping("/{id}")
    public ApiResponse<Pond> detail(@PathVariable Long id) {
        return ApiResponse.success(service.get(id));
    }

    @PostMapping
    public ApiResponse<Pond> create(@RequestBody Pond in) {
        return ApiResponse.success(service.create(in));
    }

    @PutMapping("/{id}")
    public ApiResponse<Pond> update(@PathVariable Long id, @RequestBody Pond in) {
        return ApiResponse.success(service.update(id, in));
    }

    @PostMapping("/{id}/status")
    public ApiResponse<Pond> updateStatus(@PathVariable Long id, @RequestParam String status) {
        return ApiResponse.success(service.updateStatus(id, status));
    }
}
