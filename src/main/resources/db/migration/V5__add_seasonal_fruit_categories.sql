INSERT INTO product_category (name, code, sort_order, status) VALUES
    ('甜蜜果园', 'sweet-orchard', 2, 1),
    ('缤纷莓果', 'berry-selection', 3, 1),
    ('热带鲜享', 'tropical-fresh', 4, 1),
    ('柑橘飘香', 'citrus-garden', 5, 1);

SET @sweet_orchard_category_id = (
    SELECT id FROM product_category WHERE code = 'sweet-orchard'
);
SET @berry_selection_category_id = (
    SELECT id FROM product_category WHERE code = 'berry-selection'
);
SET @tropical_fresh_category_id = (
    SELECT id FROM product_category WHERE code = 'tropical-fresh'
);
SET @citrus_garden_category_id = (
    SELECT id FROM product_category WHERE code = 'citrus-garden'
);

INSERT INTO mall_product (
    category_id, name, subtitle, image_path, price, original_price,
    unit, stock, sales, sort_order, status
) VALUES
    (@sweet_orchard_category_id, '洛川红富士', '脆甜多汁，果香浓郁', '/images/products/seasonal-fruit-placeholder.jpg',
     18.90, ROUND(18.90 * 1.20, 2), '约2.5kg/箱', 240, 138, 1, 1),
    (@sweet_orchard_category_id, '新疆香梨', '皮薄肉细，清甜爽口', '/images/products/seasonal-fruit-placeholder.jpg',
     24.90, ROUND(24.90 * 1.20, 2), '约2kg/箱', 210, 112, 2, 1),
    (@berry_selection_category_id, '丹东红颜草莓', '香甜柔嫩，颗颗饱满', '/images/products/seasonal-fruit-placeholder.jpg',
     36.90, ROUND(36.90 * 1.20, 2), '约500g/盒', 160, 156, 1, 1),
    (@berry_selection_category_id, '云南高山蓝莓', '酸甜适口，清新爆汁', '/images/products/seasonal-fruit-placeholder.jpg',
     32.90, ROUND(32.90 * 1.20, 2), '约250g/盒', 150, 97, 2, 1),
    (@tropical_fresh_category_id, '海南金钻凤梨', '香气浓郁，甜润少酸', '/images/products/seasonal-fruit-placeholder.jpg',
     25.90, ROUND(25.90 * 1.20, 2), '约1.5kg/个', 190, 88, 1, 1),
    (@tropical_fresh_category_id, '红心火龙果', '细腻清甜，鲜嫩多汁', '/images/products/seasonal-fruit-placeholder.jpg',
     21.90, ROUND(21.90 * 1.20, 2), '约2.5kg/箱', 180, 76, 2, 1),
    (@citrus_garden_category_id, '赣南脐橙', '橙香清新，汁水丰盈', '/images/products/seasonal-fruit-placeholder.jpg',
     23.90, ROUND(23.90 * 1.20, 2), '约2.5kg/箱', 260, 149, 1, 1),
    (@citrus_garden_category_id, '南丰蜜桔', '皮薄易剥，甜蜜化渣', '/images/products/seasonal-fruit-placeholder.jpg',
     16.90, ROUND(16.90 * 1.20, 2), '约2.5kg/箱', 230, 121, 2, 1);
