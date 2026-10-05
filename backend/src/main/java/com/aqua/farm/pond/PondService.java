package com.aqua.farm.pond;

import com.aqua.farm.common.BizException;
import com.aqua.farm.common.Dict;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PondService {

    private final PondRepository repo;

    public PondService(PondRepository repo) {
        this.repo = repo;
    }

    public List<Pond> list(String status) {
        if (status != null && !status.isBlank()) {
            if (!Dict.isValidPondStatus(status)) {
                throw new BizException("塘口状态必须是：在养 / 清塘 / 休整 / 废弃");
            }
            return repo.findByStatus(status);
        }
        return repo.findAll();
    }

    public Pond get(Long id) {
        return repo.findById(id).orElseThrow(() -> new BizException("塘口不存在"));
    }

    public Pond create(Pond in) {
        String pondNo = in.getPondNo() == null ? null : in.getPondNo().trim();
        if (pondNo == null || pondNo.isEmpty()) {
            throw new BizException("塘号必填");
        }
        if (repo.existsByPondNo(pondNo)) {
            throw new BizException("塘号已存在：" + pondNo);
        }
        if (in.getArea() == null || in.getArea().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("塘口面积必须大于 0");
        }
        if (in.getDepth() != null && in.getDepth().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("塘口水深必须大于 0");
        }
        if (!Dict.isValidFishType(in.getFishType())) {
            throw new BizException("养殖品种必须是：草鱼 / 鲢鱼 / 鲫鱼 / 南美白虾 / 河蟹");
        }
        Pond p = new Pond();
        p.setPondNo(pondNo);
        p.setArea(in.getArea());
        p.setDepth(in.getDepth() == null ? new BigDecimal("1.8") : in.getDepth());
        p.setFishType(in.getFishType());
        String status = (in.getStatus() == null || in.getStatus().isBlank()) ? Dict.POND_ACTIVE : in.getStatus();
        if (!Dict.isValidPondStatus(status)) {
            throw new BizException("塘口状态必须是：在养 / 清塘 / 休整 / 废弃");
        }
        p.setStatus(status);
        p.setStockQty(in.getStockQty() == null || in.getStockQty() < 0 ? 0 : in.getStockQty());
        p.setCreatedAt(LocalDateTime.now());
        p.setUpdatedAt(LocalDateTime.now());
        return repo.save(p);
    }

    public Pond update(Long id, Pond in) {
        Pond p = get(id);
        if (Dict.isPondDead(p.getStatus())) {
            throw new BizException("废弃塘口不可再做任何作业");
        }
        if (in.getPondNo() != null && !in.getPondNo().isBlank()) {
            String pondNo = in.getPondNo().trim();
            if (!pondNo.equals(p.getPondNo()) && repo.existsByPondNo(pondNo)) {
                throw new BizException("塘号已存在：" + pondNo);
            }
            p.setPondNo(pondNo);
        }
        if (in.getArea() != null) {
            if (in.getArea().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BizException("塘口面积必须大于 0");
            }
            p.setArea(in.getArea());
        }
        if (in.getDepth() != null) {
            if (in.getDepth().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BizException("塘口水深必须大于 0");
            }
            p.setDepth(in.getDepth());
        }
        if (in.getFishType() != null) {
            if (!Dict.isValidFishType(in.getFishType())) {
                throw new BizException("养殖品种必须是：草鱼 / 鲢鱼 / 鲫鱼 / 南美白虾 / 河蟹");
            }
            p.setFishType(in.getFishType());
        }
        if (in.getStatus() != null && !in.getStatus().isBlank()) {
            if (!Dict.canTransferPond(p.getStatus(), in.getStatus())) {
                throw new BizException("塘口状态流转非法：" + p.getStatus() + " 不可变更为 " + in.getStatus() + "，废弃为终态");
            }
            p.setStatus(in.getStatus());
        }
        p.setUpdatedAt(LocalDateTime.now());
        return repo.save(p);
    }

    /** 状态流转：废弃为终态不可恢复。 */
    public Pond updateStatus(Long id, String to) {
        Pond p = get(id);
        if (!Dict.canTransferPond(p.getStatus(), to)) {
            throw new BizException("塘口状态流转非法：" + p.getStatus() + " 不可变更为 " + to + "，废弃为终态");
        }
        p.setStatus(to);
        p.setUpdatedAt(LocalDateTime.now());
        return repo.save(p);
    }

    /** 存活塘口校验：投放鱼苗 / 捕捞出塘都要求塘口处于「在养」。 */
    public Pond requireActive(Long pondId) {
        Pond p = get(pondId);
        if (Dict.isPondDead(p.getStatus())) {
            throw new BizException("废弃塘口不可再做任何作业");
        }
        if (!Dict.isPondActive(p.getStatus())) {
            throw new BizException("只有「在养」的塘口可作业，当前塘口状态为：" + p.getStatus());
        }
        return p;
    }

    /** 投放后累加塘口存栏。 */
    public Pond addStock(Long pondId, int delta) {
        Pond p = get(pondId);
        int current = p.getStockQty() == null ? 0 : p.getStockQty();
        p.setStockQty(current + delta);
        p.setUpdatedAt(LocalDateTime.now());
        return repo.save(p);
    }

    /** 捕捞后扣减塘口存栏，存栏不足则拒绝。 */
    public Pond reduceStock(Long pondId, int delta) {
        Pond p = get(pondId);
        int current = p.getStockQty() == null ? 0 : p.getStockQty();
        if (current < delta) {
            throw new BizException("存栏不足：塘口「" + p.getPondNo() + "」当前存栏 " + current + " 尾，本次需出塘 " + delta + " 尾");
        }
        p.setStockQty(current - delta);
        p.setUpdatedAt(LocalDateTime.now());
        return repo.save(p);
    }
}
