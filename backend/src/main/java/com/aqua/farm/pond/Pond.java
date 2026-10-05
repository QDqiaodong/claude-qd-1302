package com.aqua.farm.pond;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Data;

/** 塘口：养殖场的基本生产单元，一口塘对应一份存栏。 */
@Data
@Entity
@Table(name = "pond")
public class Pond {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 塘号，全场唯一 */
    @Column(name = "pond_no", unique = true)
    private String pondNo;

    /** 面积（亩），必须大于 0 */
    private BigDecimal area;

    /** 水深（米） */
    private BigDecimal depth;

    /** 养殖品种：草鱼 / 鲢鱼 / 鲫鱼 / 南美白虾 / 河蟹 */
    @Column(name = "fish_type")
    private String fishType;

    /** 状态：在养 / 清塘 / 休整 / 废弃 */
    private String status;

    /** 存栏尾数，随投放累加、随捕捞扣减 */
    @Column(name = "stock_qty")
    private Integer stockQty;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
}
