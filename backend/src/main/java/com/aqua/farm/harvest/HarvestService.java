package com.aqua.farm.harvest;

import com.aqua.farm.common.BizException;
import com.aqua.farm.common.Dict;
import com.aqua.farm.pond.Pond;
import com.aqua.farm.pond.PondService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class HarvestService {

    private final HarvestRepository repo;
    private final PondService pondService;

    public HarvestService(HarvestRepository repo, PondService pondService) {
        this.repo = repo;
        this.pondService = pondService;
    }

    public List<Harvest> list(Long pondId) {
        if (pondId != null) {
            return repo.findByPondIdOrderByHarvestDateDesc(pondId);
        }
        return repo.findAll();
    }

    public Harvest create(Harvest in) {
        if (in.getPondId() == null) {
            throw new BizException("捕捞出塘必须指定塘口");
        }
        // 只能捕「在养」的塘口
        pondService.requireActive(in.getPondId());
        if (in.getQty() == null || in.getQty().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("出塘斤数必须大于 0");
        }
        if (in.getPricePerJin() == null || in.getPricePerJin().compareTo(BigDecimal.ZERO) < 0) {
            throw new BizException("收购单价必填且不能为负数");
        }
        if (in.getAvgSpec() != null && in.getAvgSpec().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("均重（斤/尾）必须大于 0");
        }
        Harvest h = new Harvest();
        h.setPondId(in.getPondId());
        h.setHarvestDate(in.getHarvestDate() == null ? LocalDate.now() : in.getHarvestDate());
        h.setQty(in.getQty());
        h.setAvgSpec(in.getAvgSpec());
        h.setBuyer(in.getBuyer() == null || in.getBuyer().isBlank() ? "自销" : in.getBuyer().trim());
        h.setPricePerJin(in.getPricePerJin());
        // 金额一律服务端计算：斤数 × 单价
        h.setAmount(calcAmount(h.getQty(), h.getPricePerJin()));
        h.setCreatedAt(LocalDateTime.now());
        h.setUpdatedAt(LocalDateTime.now());
        // 存栏不足直接拒绝（出塘尾数 = 斤数 ÷ 均重，无均重时按 1 斤/尾折算）
        pondService.reduceStock(h.getPondId(), toTails(h.getQty(), h.getAvgSpec()));
        return repo.save(h);
    }

    public Harvest update(Long id, Harvest in) {
        Harvest h = repo.findById(id).orElseThrow(() -> new BizException("出塘记录不存在"));
        Pond pond = pondService.get(h.getPondId());
        if (Dict.isPondDead(pond.getStatus())) {
            throw new BizException("废弃塘口不可再做任何作业，无法修改出塘记录");
        }
        BigDecimal qty = h.getQty();
        BigDecimal avgSpec = h.getAvgSpec();
        if (in.getQty() != null) {
            if (in.getQty().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BizException("出塘斤数必须大于 0");
            }
            qty = in.getQty();
        }
        if (in.getAvgSpec() != null) {
            if (in.getAvgSpec().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BizException("均重（斤/尾）必须大于 0");
            }
            avgSpec = in.getAvgSpec();
        }
        if (in.getHarvestDate() != null) {
            h.setHarvestDate(in.getHarvestDate());
        }
        if (in.getBuyer() != null && !in.getBuyer().isBlank()) {
            h.setBuyer(in.getBuyer().trim());
        }
        BigDecimal price = h.getPricePerJin();
        if (in.getPricePerJin() != null) {
            if (in.getPricePerJin().compareTo(BigDecimal.ZERO) < 0) {
                throw new BizException("收购单价不能为负数");
            }
            price = in.getPricePerJin();
        }
        // 出塘尾数变化部分同步回塘口存栏
        int oldTails = toTails(h.getQty(), h.getAvgSpec());
        int newTails = toTails(qty, avgSpec);
        if (newTails != oldTails) {
            if (newTails > oldTails) {
                pondService.reduceStock(h.getPondId(), newTails - oldTails);
            } else {
                pondService.addStock(h.getPondId(), oldTails - newTails);
            }
        }
        h.setQty(qty);
        h.setAvgSpec(avgSpec);
        h.setPricePerJin(price);
        h.setAmount(calcAmount(qty, price));
        h.setUpdatedAt(LocalDateTime.now());
        return repo.save(h);
    }

    /** 金额 = 斤数 × 单价，保留 2 位小数。 */
    private static BigDecimal calcAmount(BigDecimal qty, BigDecimal price) {
        return qty.multiply(price).setScale(2, RoundingMode.HALF_UP);
    }

    /** 出塘尾数：有均重时按 斤数 ÷ 均重 折算，否则按 1 斤/尾计。 */
    private static int toTails(BigDecimal qty, BigDecimal avgSpec) {
        if (qty == null) {
            return 0;
        }
        if (avgSpec == null || avgSpec.compareTo(BigDecimal.ZERO) <= 0) {
            return qty.setScale(0, RoundingMode.HALF_UP).intValue();
        }
        return qty.divide(avgSpec, 0, RoundingMode.HALF_UP).intValue();
    }
}
