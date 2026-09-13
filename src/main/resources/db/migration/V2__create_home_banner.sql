CREATE TABLE home_banner (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    title VARCHAR(64) NOT NULL,
    image_path VARCHAR(255) NOT NULL,
    sort_order INT UNSIGNED NOT NULL DEFAULT 0,
    status TINYINT UNSIGNED NOT NULL DEFAULT 1,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_home_banner_status_sort (status, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO home_banner (title, image_path, sort_order, status) VALUES
    ('秋日石榴', '/images/banners/autumn-pomegranate.jpg', 1, 1),
    ('紫玉葡萄', '/images/banners/autumn-grapes.jpg', 2, 1),
    ('黄金秋梨', '/images/banners/autumn-asian-pear.jpg', 3, 1);
