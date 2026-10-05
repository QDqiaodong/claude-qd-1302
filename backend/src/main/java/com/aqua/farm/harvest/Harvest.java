package com.aqua.farm.harvest;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.Data;

/** 捕捞出塘：一批出塘记一条，金额由服务端按 斤数 × 单价 计算，不信前端传入值。 */
@Data
@Entity
@Table(name = "harvest")
public class Harvest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 出塘塘口 id */
    @Column(name = "pond_id")
    private Long pondId;

    /** 出塘日期 */
    @Column(name = "harvest_date")
    private LocalDate harvestDate;

    /** 出塘斤数，必须大于 0 */
    private BigDecimal qty;

    /** 均重（斤/尾） */
    @Column(name = "avg_spec")
    private BigDecimal avgSpec;

    /** 收购商 */
    private String buyer;

    /** 单价（元/斤） */
    @Column(name = "price_per_jin")
    private BigDecimal pricePerJin;

    /** 金额（元），服务端计算 = 斤数 × 单价 */
    private BigDecimal amount;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
}
