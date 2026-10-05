package com.aqua.farm.stocking;

import com.aqua.farm.common.BizException;
import com.aqua.farm.common.Dict;
import com.aqua.farm.pond.PondService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class StockingService {

    private static final BigDecimal RATE_MIN = new BigDecimal("0");
    private static final BigDecimal RATE_MAX = new BigDecimal("100");

    private final StockingRepository repo;
    private final PondService pondService;

    public StockingService(StockingRepository repo, PondService pondService) {
        this.repo = repo;
        this.pondService = pondService;
    }

    public List<Stocking> list(Long pondId) {
        if (pondId != null) {
            return repo.findByPondIdOrderByReleaseDateDesc(pondId);
        }
        return repo.findAll();
    }

    public Stocking create(Stocking in) {
        if (in.getPondId() == null) {
            throw new BizException("鱼苗投放必须指定塘口");
        }
        // 只能投到「在养」的塘口（顺带拦截废弃塘）
        pondService.requireActive(in.getPondId());
        if (!Dict.isValidFishType(in.getFryType())) {
            throw new BizException("鱼苗品种必须是：草鱼 / 鲢鱼 / 鲫鱼 / 南美白虾 / 河蟹");
        }
        if (in.getQty() == null || in.getQty() <= 0) {
            throw new BizException("投放尾数必须大于 0");
        }
        if (in.getSpec() == null || in.getSpec().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("投放规格（尾/斤）必须大于 0");
        }
        if (in.getSurvivalRate() != null
                && (in.getSurvivalRate().compareTo(RATE_MIN) < 0 || in.getSurvivalRate().compareTo(RATE_MAX) > 0)) {
            throw new BizException("成活率必须在 0~100 之间");
        }
        Stocking s = new Stocking();
        s.setPondId(in.getPondId());
        s.setFryType(in.getFryType());
        s.setQty(in.getQty());
        s.setSpec(in.getSpec());
        s.setReleaseDate(in.getReleaseDate() == null ? LocalDate.now() : in.getReleaseDate());
        s.setSupplier(in.getSupplier() == null || in.getSupplier().isBlank() ? "自繁自育" : in.getSupplier().trim());
        s.setSurvivalRate(in.getSurvivalRate() == null ? new BigDecimal("90.0") : in.getSurvivalRate());
        s.setCreatedAt(LocalDateTime.now());
        s.setUpdatedAt(LocalDateTime.now());
        Stocking saved = repo.save(s);
        // 投放后累加塘口存栏
        pondService.addStock(saved.getPondId(), saved.getQty());
        return saved;
    }

    public Stocking update(Long id, Stocking in) {
        Stocking s = repo.findById(id).orElseThrow(() -> new BizException("投放记录不存在"));
        if (in.getFryType() != null) {
            if (!Dict.isValidFishType(in.getFryType())) {
                throw new BizException("鱼苗品种必须是：草鱼 / 鲢鱼 / 鲫鱼 / 南美白虾 / 河蟹");
            }
            s.setFryType(in.getFryType());
        }
        if (in.getQty() != null) {
            if (in.getQty() <= 0) {
                throw new BizException("投放尾数必须大于 0");
            }
            int delta = in.getQty() - (s.getQty() == null ? 0 : s.getQty());
            s.setQty(in.getQty());
            if (delta > 0) {
                pondService.addStock(s.getPondId(), delta);
            } else if (delta < 0) {
                pondService.reduceStock(s.getPondId(), -delta);
            }
        }
        if (in.getSpec() != null) {
            if (in.getSpec().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BizException("投放规格（尾/斤）必须大于 0");
            }
            s.setSpec(in.getSpec());
        }
        if (in.getReleaseDate() != null) {
            s.setReleaseDate(in.getReleaseDate());
        }
        if (in.getSupplier() != null && !in.getSupplier().isBlank()) {
            s.setSupplier(in.getSupplier().trim());
        }
        if (in.getSurvivalRate() != null) {
            if (in.getSurvivalRate().compareTo(RATE_MIN) < 0 || in.getSurvivalRate().compareTo(RATE_MAX) > 0) {
                throw new BizException("成活率必须在 0~100 之间");
            }
            s.setSurvivalRate(in.getSurvivalRate());
        }
        s.setUpdatedAt(LocalDateTime.now());
        return repo.save(s);
    }
}
