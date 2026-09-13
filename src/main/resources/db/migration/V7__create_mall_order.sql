CREATE TABLE mall_order (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    order_no VARCHAR(40) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    user_id BIGINT UNSIGNED NOT NULL,
    status TINYINT UNSIGNED NOT NULL DEFAULT 1,
    total_amount DECIMAL(12, 2) UNSIGNED NOT NULL,
    total_quantity INT UNSIGNED NOT NULL,
    receiver_name VARCHAR(64) NOT NULL,
    receiver_phone VARCHAR(20) NOT NULL,
    receiver_address VARCHAR(255) NOT NULL,
    remark VARCHAR(255) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_mall_order_order_no (order_no),
    KEY idx_mall_order_user_created (user_id, created_at),
    CONSTRAINT fk_mall_order_user
        FOREIGN KEY (user_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE mall_order_item (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    order_id BIGINT UNSIGNED NOT NULL,
    product_id BIGINT UNSIGNED NOT NULL,
    product_name VARCHAR(128) NOT NULL,
    product_image_path VARCHAR(255) NOT NULL,
    price DECIMAL(10, 2) UNSIGNED NOT NULL,
    original_price DECIMAL(10, 2) UNSIGNED NOT NULL,
    unit VARCHAR(32) NOT NULL,
    quantity INT UNSIGNED NOT NULL,
    subtotal DECIMAL(12, 2) UNSIGNED NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_mall_order_item_order (order_id),
    CONSTRAINT fk_mall_order_item_order
        FOREIGN KEY (order_id) REFERENCES mall_order (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
