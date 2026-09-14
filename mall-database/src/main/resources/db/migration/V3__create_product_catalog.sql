CREATE TABLE product_category (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    name VARCHAR(64) NOT NULL,
    code VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    sort_order INT UNSIGNED NOT NULL DEFAULT 0,
    status TINYINT UNSIGNED NOT NULL DEFAULT 1,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_product_category_code (code),
    KEY idx_product_category_status_sort (status, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE mall_product (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    category_id BIGINT UNSIGNED NOT NULL,
    name VARCHAR(128) NOT NULL,
    subtitle VARCHAR(255) NOT NULL,
    image_path VARCHAR(255) NOT NULL,
    price DECIMAL(10, 2) UNSIGNED NOT NULL,
    unit VARCHAR(32) NOT NULL,
    stock INT UNSIGNED NOT NULL DEFAULT 0,
    sales INT UNSIGNED NOT NULL DEFAULT 0,
    sort_order INT UNSIGNED NOT NULL DEFAULT 0,
    status TINYINT UNSIGNED NOT NULL DEFAULT 1,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_mall_product_category_status_sort (category_id, status, sort_order),
    CONSTRAINT fk_mall_product_category
        FOREIGN KEY (category_id) REFERENCES product_category (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO product_category (name, code, sort_order, status)
VALUES ('新鲜热卖', 'fresh-hot', 1, 1);

SET @fresh_hot_category_id = LAST_INSERT_ID();

INSERT INTO mall_product (
    category_id, name, subtitle, image_path, price, unit, stock, sales, sort_order, status
) VALUES
    (@fresh_hot_category_id, '当季软籽石榴', '果粒饱满，籽软汁多', '/images/products/soft-seed-pomegranate.jpg', 29.90, '约2.5kg/箱', 200, 128, 1, 1),
    (@fresh_hot_category_id, '阳光玫瑰葡萄', '清甜脆嫩，淡雅玫瑰香', '/images/products/shine-muscat-grapes.jpg', 39.90, '约1kg/盒', 150, 96, 2, 1),
    (@fresh_hot_category_id, '秋月梨', '细腻多汁，清甜爽口', '/images/products/autumn-moon-pear.jpg', 26.90, '约2.5kg/箱', 220, 165, 3, 1),
    (@fresh_hot_category_id, '脆甜柿子', '脆而不涩，香甜可口', '/images/products/crisp-persimmon.jpg', 19.90, '约2.5kg/箱', 180, 82, 4, 1),
    (@fresh_hot_category_id, '当季蜜柚', '果肉饱满，清甜微酸', '/images/products/honey-pomelo.jpg', 16.90, '约1.5kg/个', 260, 143, 5, 1);
