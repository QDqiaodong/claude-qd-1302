package com.aqua.farm.common;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 水产养殖场业务词典与规则参数表：塘口养殖品种 / 状态流转、鱼苗投放品种、
 * 水质检测判定区间 / 状态，以及服务端强制计算的规则阈值。规则集中在此，便于出题时被要求调整或扩展。
 */
public final class Dict {

    private Dict() {
    }

    // ---------------- 塘口 pond ----------------
    /** 草鱼 / 鲢鱼 / 鲫鱼 / 南美白虾 / 河蟹 */
    public static final List<String> FISH_TYPE = List.of("草鱼", "鲢鱼", "鲫鱼", "南美白虾", "河蟹");

    /** 在养 / 清塘 / 休整 / 废弃 */
    public static final List<String> POND_STATUS = List.of("在养", "清塘", "休整", "废弃");

    /** 可作业（可投放鱼苗、可捕捞出塘）的塘口状态：只有「在养」 */
    public static final String POND_ACTIVE = "在养";

    /** 终态塘口：不可再做任何作业 */
    public static final String POND_DEAD = "废弃";

    /** 塘口状态机：在养→清塘→休整→在养；任一状态可判废；废弃为终态不可恢复 */
    public static final Map<String, List<String>> POND_FLOW = Map.of(
            "在养", List.of("清塘", "休整", "废弃"),
            "清塘", List.of("休整", "在养", "废弃"),
            "休整", List.of("在养", "清塘", "废弃"),
            "废弃", List.of());

    // ---------------- 水质检测 water ----------------
    /** 正常 / 预警 / 超标 */
    public static final List<String> WATER_STATUS = List.of("正常", "预警", "超标");

    /** 溶氧（mg/L）低于该值判「超标」 */
    public static final BigDecimal OXYGEN_BAD = new BigDecimal("3.0");

    /** 溶氧（mg/L）低于该值判「预警」 */
    public static final BigDecimal OXYGEN_ALERT = new BigDecimal("4.5");

    /** pH 下限，低于判「超标」 */
    public static final BigDecimal PH_MIN = new BigDecimal("6.5");

    /** pH 上限，高于判「超标」 */
    public static final BigDecimal PH_MAX = new BigDecimal("9.0");

    /** 水温适宜区间（℃），仅作塘口卡三环展示参考，不参与状态判定 */
    public static final BigDecimal TEMP_MIN = new BigDecimal("10");

    /** 水温适宜上限（℃），仅作塘口卡三环展示参考，不参与状态判定 */
    public static final BigDecimal TEMP_MAX = new BigDecimal("35");

    /** 溶氧正常上限（mg/L），仅作三环刻度参考 */
    public static final BigDecimal OXYGEN_MAX = new BigDecimal("12");

    // ---------------- 取值方法 ----------------

    public static boolean isValidFishType(String t) {
        return t != null && FISH_TYPE.contains(t);
    }

    public static boolean isValidPondStatus(String s) {
        return s != null && POND_STATUS.contains(s);
    }

    public static boolean isValidWaterStatus(String s) {
        return s != null && WATER_STATUS.contains(s);
    }

    /**
     * 塘口是否可作业：只有「在养」的塘口可投放鱼苗、可捕捞出塘。
     */
    public static boolean isPondActive(String status) {
        return POND_ACTIVE.equals(status);
    }

    /**
     * 塘口是否已判废：废弃塘不可再做任何作业（投放 / 检测 / 捕捞 / 修改）。
     */
    public static boolean isPondDead(String status) {
        return POND_DEAD.equals(status);
    }

    /**
     * 塘口状态流转校验：废弃为终态不可恢复，其余按 POND_FLOW 前进。
     */
    public static boolean canTransferPond(String from, String to) {
        if (!isValidPondStatus(from) || !isValidPondStatus(to)) {
            return false;
        }
        List<String> next = POND_FLOW.get(from);
        return next != null && next.contains(to);
    }

    /**
     * 水质状态由服务端计算，不信前端传入值：
     * 溶氧 &lt; 3.0 或 pH &lt; 6.5 或 pH &gt; 9.0 → 超标；溶氧 &lt; 4.5 → 预警；其余 → 正常。
     * 指标为空时视为未检测该项，不参与判定。
     */
    public static String calcWaterStatus(BigDecimal oxygen, BigDecimal ph) {
        if (oxygen != null && oxygen.compareTo(OXYGEN_BAD) < 0) {
            return "超标";
        }
        if (ph != null && (ph.compareTo(PH_MIN) < 0 || ph.compareTo(PH_MAX) > 0)) {
            return "超标";
        }
        if (oxygen != null && oxygen.compareTo(OXYGEN_ALERT) < 0) {
            return "预警";
        }
        return "正常";
    }
}
