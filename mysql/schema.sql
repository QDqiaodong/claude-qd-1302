SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
SET character_set_client = utf8mb4;
SET character_set_connection = utf8mb4;
SET character_set_results = utf8mb4;

-- ---------------------------------------------------------------------
-- 水产养殖场 · 4 模块业务库
-- 库名 aqua_farm；application.yml 里 ddl-auto: none，表结构全部由本文件在 MySQL 镜像构建期导入。
-- 注意：mysql 容器是 UTC，时间戳一律写 NOW()，日期用 DATE_SUB(CURDATE(), INTERVAL N DAY)。
-- 两处 schema（backend/src/main/resources/schema.sql 与 mysql/schema.sql）内容必须完全一致。

CREATE DATABASE IF NOT EXISTS aqua_farm
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE aqua_farm;

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS harvest;
DROP TABLE IF EXISTS water;
DROP TABLE IF EXISTS stocking;
DROP TABLE IF EXISTS pond;
SET FOREIGN_KEY_CHECKS = 1;

-- ---------------- 1. 塘口 pond（父表，必须先建） ----------------
CREATE TABLE pond (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    pond_no      VARCHAR(32) NOT NULL UNIQUE COMMENT '塘号，全场唯一',
    area         DECIMAL(10,2) NOT NULL COMMENT '面积（亩），必须大于 0',
    depth        DECIMAL(10,2) COMMENT '水深（米）',
    fish_type    VARCHAR(16) COMMENT '养殖品种：草鱼 / 鲢鱼 / 鲫鱼 / 南美白虾 / 河蟹',
    status       VARCHAR(16) COMMENT '状态：在养 / 清塘 / 休整 / 废弃',
    stock_qty    INT NOT NULL DEFAULT 0 COMMENT '存栏尾数，投放累加、捕捞扣减',
    created_at   DATETIME COMMENT '创建时间',
    updated_at   DATETIME COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------- 2. 鱼苗投放 stocking ----------------
CREATE TABLE stocking (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    pond_id       BIGINT NOT NULL COMMENT '投放塘口 id',
    fry_type      VARCHAR(16) COMMENT '鱼苗品种：草鱼 / 鲢鱼 / 鲫鱼 / 南美白虾 / 河蟹',
    qty           INT NOT NULL COMMENT '投放尾数，必须大于 0',
    spec          DECIMAL(10,2) COMMENT '规格（尾/斤），必须大于 0',
    release_date  DATE COMMENT '投放日期',
    supplier      VARCHAR(64) COMMENT '鱼苗供应商',
    survival_rate DECIMAL(5,2) COMMENT '成活率（%），必须在 0~100 之间',
    created_at    DATETIME COMMENT '创建时间',
    updated_at    DATETIME COMMENT '更新时间',
    CONSTRAINT fk_stocking_pond FOREIGN KEY (pond_id) REFERENCES pond (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------- 3. 水质检测 water ----------------
CREATE TABLE water (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    pond_id      BIGINT NOT NULL COMMENT '检测塘口 id',
    check_date   DATE COMMENT '检测日期',
    temperature  DECIMAL(5,2) COMMENT '水温（℃）',
    ph           DECIMAL(4,2) COMMENT 'pH 值',
    oxygen       DECIMAL(5,2) COMMENT '溶氧（mg/L）',
    status       VARCHAR(16) COMMENT '状态：正常 / 预警 / 超标（服务端按溶氧与 pH 计算）',
    created_at   DATETIME COMMENT '创建时间',
    updated_at   DATETIME COMMENT '更新时间',
    CONSTRAINT fk_water_pond FOREIGN KEY (pond_id) REFERENCES pond (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------- 4. 捕捞出塘 harvest ----------------
CREATE TABLE harvest (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    pond_id       BIGINT NOT NULL COMMENT '出塘塘口 id',
    harvest_date  DATE COMMENT '出塘日期',
    qty           DECIMAL(10,2) NOT NULL COMMENT '出塘斤数，必须大于 0',
    avg_spec      DECIMAL(10,3) COMMENT '均重（斤/尾）',
    buyer         VARCHAR(64) COMMENT '收购商',
    price_per_jin DECIMAL(10,2) COMMENT '收购单价（元/斤）',
    amount        DECIMAL(12,2) COMMENT '金额（元），服务端计算 = 斤数 × 单价',
    created_at    DATETIME COMMENT '创建时间',
    updated_at    DATETIME COMMENT '更新时间',
    CONSTRAINT fk_harvest_pond FOREIGN KEY (pond_id) REFERENCES pond (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------
-- 种子数据（含异常态，用于真实触发业务拒绝分支）
--   异常态 1：P05 五号塘为「废弃」→ 投放 / 检测 / 捕捞 / 修改一律拦截（W5 为其异常检测记录）
--   异常态 2：P03 三号塘为「清塘」→ 非在养塘口不可投放、不可捕捞（S4 / H5 为其异常记录）
--   异常态 3：W3 溶氧 2.60 且 pH 6.20 → 超标；W4 pH 9.40 → 超标；W2 溶氧 3.80 → 预警
--   异常态 4：S4 投放尾数 0、S5 成活率 105.00（超出 0~100）→ 新增 / 修改同类数据被拦截
--   异常态 5：H5 在清塘塘口出塘、H6 金额 999.00 与 斤数×单价 不符 → 服务端重算纠正
-- ---------------------------------------------------------------------

INSERT IGNORE INTO pond (pond_no, area, depth, fish_type, status, stock_qty, created_at, updated_at) VALUES
('P01', 12.50, 2.20, '草鱼',     '在养', 8000,  NOW(), NOW()),
('P02', 8.00,  1.80, '南美白虾', '在养', 6000,  NOW(), NOW()),
('P03', 10.00, 2.00, '鲢鱼',     '清塘', 0,     NOW(), NOW()),
('P04', 6.50,  1.60, '鲫鱼',     '休整', 0,     NOW(), NOW()),
('P05', 5.00,  1.20, '河蟹',     '废弃', 0,     NOW(), NOW()),
('P06', 15.00, 2.50, '河蟹',     '在养', 3000,  NOW(), NOW());

INSERT IGNORE INTO stocking (pond_id, fry_type, qty, spec, release_date, supplier, survival_rate, created_at, updated_at) VALUES
(1, '草鱼',     5000, 8.00,  DATE_SUB(CURDATE(), INTERVAL 90 DAY),  '江城鱼苗场',   92.00, NOW(), NOW()),
(1, '草鱼',     3000, 10.00, DATE_SUB(CURDATE(), INTERVAL 60 DAY),  '江城鱼苗场',   88.50, NOW(), NOW()),
(2, '南美白虾', 6000, 120.00, DATE_SUB(CURDATE(), INTERVAL 45 DAY), '南方虾苗公司', 85.00, NOW(), NOW()),
(3, '鲢鱼',     2000, 6.00,  DATE_SUB(CURDATE(), INTERVAL 30 DAY),  '湖滨鱼种场',   80.00, NOW(), NOW()),
(4, '鲫鱼',     0,    5.00,  DATE_SUB(CURDATE(), INTERVAL 20 DAY),  '湖滨鱼种场',   0.00,  NOW(), NOW()),
(6, '河蟹',     3000, 60.00, DATE_SUB(CURDATE(), INTERVAL 50 DAY),  '阳澄蟹苗基地', 105.00, NOW(), NOW());

INSERT IGNORE INTO water (pond_id, check_date, temperature, ph, oxygen, status, created_at, updated_at) VALUES
(1, DATE_SUB(CURDATE(), INTERVAL 2 DAY), 26.50, 7.80, 6.20, '正常', NOW(), NOW()),
(1, DATE_SUB(CURDATE(), INTERVAL 1 DAY), 28.00, 8.60, 3.80, '预警', NOW(), NOW()),
(2, DATE_SUB(CURDATE(), INTERVAL 1 DAY), 30.50, 9.40, 5.00, '超标', NOW(), NOW()),
(3, DATE_SUB(CURDATE(), INTERVAL 3 DAY), 24.00, 6.20, 2.60, '超标', NOW(), NOW()),
(5, DATE_SUB(CURDATE(), INTERVAL 4 DAY), 22.00, 7.00, 5.50, '正常', NOW(), NOW()),
(6, DATE_SUB(CURDATE(), INTERVAL 1 DAY), 25.00, 7.40, 5.80, '正常', NOW(), NOW());

INSERT IGNORE INTO harvest (pond_id, harvest_date, qty, avg_spec, buyer, price_per_jin, amount, created_at, updated_at) VALUES
(1, DATE_SUB(CURDATE(), INTERVAL 15 DAY), 1200.00, 2.500, '江城水产批发市场', 6.50,  7800.00,  NOW(), NOW()),
(1, DATE_SUB(CURDATE(), INTERVAL 7 DAY),  800.00,  2.800, '江城水产批发市场', 7.20,  5760.00,  NOW(), NOW()),
(2, DATE_SUB(CURDATE(), INTERVAL 5 DAY),  500.00,  0.050, '南方虾业收购站',   28.00, 14000.00, NOW(), NOW()),
(6, DATE_SUB(CURDATE(), INTERVAL 3 DAY),  300.00,  0.250, '阳澄蟹行',         55.00, 16500.00, NOW(), NOW()),
(3, DATE_SUB(CURDATE(), INTERVAL 25 DAY), 200.00,  2.000, '湖滨鱼贩',         5.00,  1000.00,  NOW(), NOW()),
(1, DATE_SUB(CURDATE(), INTERVAL 2 DAY),  100.00,  2.000, '散户收购',         6.00,  999.00,   NOW(), NOW());
