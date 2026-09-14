CREATE TABLE product_sku (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    product_id BIGINT UNSIGNED NOT NULL,
    sku_code VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    sku_name VARCHAR(64) NOT NULL,
    price DECIMAL(10, 2) UNSIGNED NOT NULL,
    original_price DECIMAL(10, 2) UNSIGNED NOT NULL,
    unit VARCHAR(32) NOT NULL,
    stock INT UNSIGNED NOT NULL DEFAULT 0,
    sales INT UNSIGNED NOT NULL DEFAULT 0,
    is_default TINYINT UNSIGNED NOT NULL DEFAULT 0,
    sort_order INT UNSIGNED NOT NULL DEFAULT 0,
    status TINYINT UNSIGNED NOT NULL DEFAULT 1,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_product_sku_code (sku_code),
    UNIQUE KEY uk_product_sku_product_name (product_id, sku_name),
    KEY idx_product_sku_product_status_sort (product_id, status, sort_order),
    CONSTRAINT fk_product_sku_product
        FOREIGN KEY (product_id) REFERENCES mall_product (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO product_sku (
    product_id, sku_code, sku_name, price, original_price, unit,
    stock, sales, is_default, sort_order, status
)
SELECT id,
       CONCAT('SKU-', LPAD(id, 6, '0'), '-S'),
       CASE MOD(id, 3)
           WHEN 0 THEN '750g尝鲜装'
           WHEN 1 THEN '500g尝鲜装'
           ELSE '400g尝鲜装'
       END,
       ROUND(price * 0.55, 2),
       ROUND(ROUND(price * 0.55, 2) * 1.20, 2),
       CASE MOD(id, 3)
           WHEN 0 THEN '750g/份'
           WHEN 1 THEN '500g/份'
           ELSE '400g/份'
       END,
       FLOOR(stock * 0.35),
       FLOOR(sales * 0.35),
       0,
       1,
       status
FROM mall_product;

INSERT INTO product_sku (
    product_id, sku_code, sku_name, price, original_price, unit,
    stock, sales, is_default, sort_order, status
)
SELECT id,
       CONCAT('SKU-', LPAD(id, 6, '0'), '-M'),
       CASE MOD(id, 3)
           WHEN 0 THEN '1.5kg标准装'
           WHEN 1 THEN '1kg标准装'
           ELSE '800g标准装'
       END,
       price,
       ROUND(price * 1.20, 2),
       CASE MOD(id, 3)
           WHEN 0 THEN '1.5kg/份'
           WHEN 1 THEN '1kg/份'
           ELSE '800g/份'
       END,
       FLOOR(stock * 0.40),
       FLOOR(sales * 0.40),
       1,
       2,
       status
FROM mall_product;

INSERT INTO product_sku (
    product_id, sku_code, sku_name, price, original_price, unit,
    stock, sales, is_default, sort_order, status
)
SELECT id,
       CONCAT('SKU-', LPAD(id, 6, '0'), '-L'),
       CASE MOD(id, 3)
           WHEN 0 THEN '3kg家庭装'
           WHEN 1 THEN '2.5kg家庭装'
           ELSE '2kg家庭装'
       END,
       ROUND(price * CASE MOD(id, 3) WHEN 0 THEN 1.90 ELSE 2.20 END, 2),
       ROUND(ROUND(price * CASE MOD(id, 3) WHEN 0 THEN 1.90 ELSE 2.20 END, 2) * 1.20, 2),
       CASE MOD(id, 3)
           WHEN 0 THEN '3kg/份'
           WHEN 1 THEN '2.5kg/份'
           ELSE '2kg/份'
       END,
       stock - FLOOR(stock * 0.35) - FLOOR(stock * 0.40),
       sales - FLOOR(sales * 0.35) - FLOOR(sales * 0.40),
       0,
       3,
       status
FROM mall_product;

ALTER TABLE cart_item
    ADD COLUMN sku_id BIGINT UNSIGNED NULL AFTER user_id;

UPDATE cart_item ci
INNER JOIN product_sku sku ON sku.product_id = ci.product_id AND sku.is_default = 1
SET ci.sku_id = sku.id;

ALTER TABLE cart_item
    DROP FOREIGN KEY fk_cart_item_product,
    DROP INDEX uk_cart_item_user_product,
    MODIFY COLUMN sku_id BIGINT UNSIGNED NOT NULL,
    ADD UNIQUE KEY uk_cart_item_user_sku (user_id, sku_id),
    ADD CONSTRAINT fk_cart_item_sku
        FOREIGN KEY (sku_id) REFERENCES product_sku (id) ON DELETE CASCADE,
    DROP COLUMN product_id;

ALTER TABLE mall_order_item
    ADD COLUMN sku_id BIGINT UNSIGNED NULL AFTER product_id,
    ADD COLUMN sku_code VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL AFTER sku_id,
    ADD COLUMN sku_name VARCHAR(64) NULL AFTER sku_code;

UPDATE mall_order_item oi
INNER JOIN product_sku sku ON sku.product_id = oi.product_id AND sku.is_default = 1
SET oi.sku_id = sku.id,
    oi.sku_code = sku.sku_code,
    oi.sku_name = sku.sku_name;

ALTER TABLE mall_order_item
    MODIFY COLUMN sku_id BIGINT UNSIGNED NOT NULL,
    MODIFY COLUMN sku_code VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    MODIFY COLUMN sku_name VARCHAR(64) NOT NULL;

ALTER TABLE mall_product
    DROP COLUMN price,
    DROP COLUMN original_price,
    DROP COLUMN unit,
    DROP COLUMN stock,
    DROP COLUMN sales;
