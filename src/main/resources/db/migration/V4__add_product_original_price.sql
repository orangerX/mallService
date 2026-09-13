ALTER TABLE mall_product
    ADD COLUMN original_price DECIMAL(10, 2) UNSIGNED NULL AFTER price;

UPDATE mall_product
SET original_price = ROUND(price * 1.20, 2);

ALTER TABLE mall_product
    MODIFY COLUMN original_price DECIMAL(10, 2) UNSIGNED NOT NULL;
