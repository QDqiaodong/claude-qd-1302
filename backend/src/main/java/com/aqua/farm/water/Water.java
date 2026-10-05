package com.aqua.farm.water;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.Data;

/** 水质检测：一次检测记一条，状态由服务端按溶氧与 pH 计算，不信前端传入值。 */
@Data
@Entity
@Table(name = "water")
public class Water {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 检测塘口 id */
    @Column(name = "pond_id")
    private Long pondId;

    /** 检测日期 */
    @Column(name = "check_date")
    private LocalDate checkDate;

    /** 水温（℃） */
    private BigDecimal temperature;

    /** pH 值 */
    private BigDecimal ph;

    /** 溶氧（mg/L） */
    private BigDecimal oxygen;

    /** 状态：正常 / 预警 / 超标（服务端计算） */
    private String status;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
}
