CREATE TABLE mall_coupon (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    name VARCHAR(64) NOT NULL,
    threshold_amount DECIMAL(10, 2) UNSIGNED NOT NULL,
    discount_amount DECIMAL(10, 2) UNSIGNED NOT NULL,
    total_quantity INT UNSIGNED NOT NULL,
    received_quantity INT UNSIGNED NOT NULL DEFAULT 0,
    start_at DATETIME(3) NOT NULL,
    end_at DATETIME(3) NOT NULL,
    status TINYINT UNSIGNED NOT NULL DEFAULT 1,
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_mall_coupon_available (status, start_at, end_at, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO mall_coupon (
    name, threshold_amount, discount_amount, total_quantity, received_quantity,
    start_at, end_at, status, sort_order
) VALUES
    ('新鲜尝鲜券', 49.00, 5.00, 1000, 0, '2020-01-01 00:00:00.000', '2099-12-31 23:59:59.999', 1, 1),
    ('水果畅享券', 99.00, 15.00, 1000, 0, '2020-01-01 00:00:00.000', '2099-12-31 23:59:59.999', 1, 2),
    ('果园大额券', 199.00, 35.00, 1000, 0, '2020-01-01 00:00:00.000', '2099-12-31 23:59:59.999', 1, 3);

CREATE TABLE user_coupon (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    coupon_id BIGINT UNSIGNED NOT NULL,
    status TINYINT UNSIGNED NOT NULL DEFAULT 1,
    received_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    used_at DATETIME(3) NULL,
    used_order_id BIGINT UNSIGNED NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_coupon_user_coupon (user_id, coupon_id),
    KEY idx_user_coupon_user_status (user_id, status, received_at),
    CONSTRAINT fk_user_coupon_user
        FOREIGN KEY (user_id) REFERENCES sys_user (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_coupon_coupon
        FOREIGN KEY (coupon_id) REFERENCES mall_coupon (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

ALTER TABLE mall_order
    ADD COLUMN user_coupon_id BIGINT UNSIGNED NULL AFTER total_amount,
    ADD COLUMN coupon_name VARCHAR(64) NULL AFTER user_coupon_id,
    ADD COLUMN coupon_discount_amount DECIMAL(10, 2) UNSIGNED NOT NULL DEFAULT 0.00 AFTER coupon_name,
    ADD COLUMN payment_amount DECIMAL(12, 2) UNSIGNED NOT NULL DEFAULT 0.00 AFTER coupon_discount_amount,
    ADD UNIQUE KEY uk_mall_order_user_coupon (user_coupon_id),
    ADD CONSTRAINT fk_mall_order_user_coupon
        FOREIGN KEY (user_coupon_id) REFERENCES user_coupon (id);

UPDATE mall_order
SET payment_amount = total_amount
WHERE user_coupon_id IS NULL;

ALTER TABLE user_coupon
    ADD CONSTRAINT fk_user_coupon_used_order
        FOREIGN KEY (used_order_id) REFERENCES mall_order (id);
