package com.aqua.farm.stocking;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.Data;

/** 鱼苗投放：一次投放记一条，投放后累加塘口存栏。 */
@Data
@Entity
@Table(name = "stocking")
public class Stocking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 投放塘口 id */
    @Column(name = "pond_id")
    private Long pondId;

    /** 鱼苗品种：草鱼 / 鲢鱼 / 鲫鱼 / 南美白虾 / 河蟹 */
    @Column(name = "fry_type")
    private String fryType;

    /** 投放尾数，必须大于 0 */
    private Integer qty;

    /** 规格（尾/斤），必须大于 0 */
    private BigDecimal spec;

    /** 投放日期 */
    @Column(name = "release_date")
    private LocalDate releaseDate;

    /** 供应商 */
    private String supplier;

    /** 成活率（%），必须在 0~100 之间 */
    @Column(name = "survival_rate")
    private BigDecimal survivalRate;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
}
