package com.aqua.farm.water;

import com.aqua.farm.common.ApiResponse;
import com.aqua.farm.common.BizException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/waters")
public class WaterController {

    private final WaterService service;

    public WaterController(WaterService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<Water>> list(@RequestParam(required = false) Long pondId) {
        return ApiResponse.success(service.list(pondId));
    }

    @GetMapping("/latest")
    public ApiResponse<Water> latest(@RequestParam(required = false) Long pondId) {
        if (pondId == null) {
            throw new BizException("请先选择要查看的塘口");
        }
        return ApiResponse.success(service.latest(pondId));
    }

    @PostMapping
    public ApiResponse<Water> create(@RequestBody Water in) {
        return ApiResponse.success(service.create(in));
    }

    @PutMapping("/{id}")
    public ApiResponse<Water> update(@PathVariable Long id, @RequestBody Water in) {
        return ApiResponse.success(service.update(id, in));
    }

    @PostMapping("/{id}/recalc")
    public ApiResponse<Water> recalc(@PathVariable Long id) {
        return ApiResponse.success(service.recalc(id));
    }
}
