package com.aqua.farm.water;

import com.aqua.farm.common.BizException;
import com.aqua.farm.common.Dict;
import com.aqua.farm.pond.Pond;
import com.aqua.farm.pond.PondService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class WaterService {

    private final WaterRepository repo;
    private final PondService pondService;

    public WaterService(WaterRepository repo, PondService pondService) {
        this.repo = repo;
        this.pondService = pondService;
    }

    public List<Water> list(Long pondId) {
        if (pondId != null) {
            return repo.findByPondIdOrderByCheckDateDesc(pondId);
        }
        return repo.findAll();
    }

    /** 某塘口最近一次检测，用于塘口卡的水质三环。 */
    public Water latest(Long pondId) {
        if (pondId == null) {
            throw new BizException("必须指定塘口");
        }
        List<Water> rows = repo.findByPondIdOrderByCheckDateDesc(pondId);
        if (rows.isEmpty()) {
            return null;
        }
        return rows.get(0);
    }

    public Water create(Water in) {
        if (in.getPondId() == null) {
            throw new BizException("水质检测必须指定塘口");
        }
        Pond pond = pondService.get(in.getPondId());
        if (Dict.isPondDead(pond.getStatus())) {
            throw new BizException("废弃塘口不可再做任何作业，无法检测水质");
        }
        Water w = new Water();
        w.setPondId(in.getPondId());
        w.setCheckDate(in.getCheckDate() == null ? LocalDate.now() : in.getCheckDate());
        w.setTemperature(in.getTemperature() == null ? new BigDecimal("25.0") : in.getTemperature());
        w.setPh(in.getPh() == null ? new BigDecimal("7.5") : in.getPh());
        w.setOxygen(in.getOxygen() == null ? new BigDecimal("6.0") : in.getOxygen());
        // 状态一律由服务端按溶氧 / pH 计算，不信前端传入值
        w.setStatus(Dict.calcWaterStatus(w.getOxygen(), w.getPh()));
        w.setCreatedAt(LocalDateTime.now());
        w.setUpdatedAt(LocalDateTime.now());
        return repo.save(w);
    }

    public Water update(Long id, Water in) {
        Water w = repo.findById(id).orElseThrow(() -> new BizException("水质检测记录不存在"));
        Pond pond = pondService.get(w.getPondId());
        if (Dict.isPondDead(pond.getStatus())) {
            throw new BizException("废弃塘口不可再做任何作业，无法修改水质记录");
        }
        if (in.getCheckDate() != null) {
            w.setCheckDate(in.getCheckDate());
        }
        if (in.getTemperature() != null) {
            w.setTemperature(in.getTemperature());
        }
        if (in.getPh() != null) {
            w.setPh(in.getPh());
        }
        if (in.getOxygen() != null) {
            w.setOxygen(in.getOxygen());
        }
        // 指标变更后重算状态
        w.setStatus(Dict.calcWaterStatus(w.getOxygen(), w.getPh()));
        w.setUpdatedAt(LocalDateTime.now());
        return repo.save(w);
    }

    /** 按服务端规则重算某条记录的状态（数据订正用）。 */
    public Water recalc(Long id) {
        Water w = repo.findById(id).orElseThrow(() -> new BizException("水质检测记录不存在"));
        w.setStatus(Dict.calcWaterStatus(w.getOxygen(), w.getPh()));
        w.setUpdatedAt(LocalDateTime.now());
        return repo.save(w);
    }
}
