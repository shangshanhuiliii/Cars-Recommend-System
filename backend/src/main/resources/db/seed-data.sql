INSERT INTO app_user (id, username, password, nickname, phone) VALUES
(1, 'demo_user', 'demo_user_password_hash_placeholder', '演示用户', NULL);

INSERT INTO admin (id, username, password, role) VALUES
(1, 'demo_admin', 'demo_admin_password_hash_placeholder', 'ADMIN');

INSERT INTO car_model (
    id, brand, series, model_name, guide_price, body_type, energy_type,
    seats, launch_year, image_url, sales_volume, user_rating, audit_status
) VALUES
(1, '比亚迪', '秦PLUS', '秦PLUS DM-i 120KM 卓越型', 99800.00, '轿车', '插混', 5, 2025, '', 35000, 4.50, 'APPROVED'),
(2, '比亚迪', '宋PLUS', '宋PLUS DM-i 110KM 旗舰型', 159800.00, 'SUV', '插混', 5, 2025, '', 28000, 4.60, 'APPROVED'),
(3, '比亚迪', '海豚', '海豚 420KM 自由版', 116800.00, '轿车', '纯电', 5, 2025, '', 24000, 4.40, 'APPROVED'),
(4, '特斯拉', 'Model Y', 'Model Y 后轮驱动版', 263900.00, 'SUV', '纯电', 5, 2025, '', 18000, 4.50, 'APPROVED'),
(5, '丰田', '卡罗拉', '卡罗拉 1.5L 先锋版', 116800.00, '轿车', '燃油', 5, 2024, '', 22000, 4.20, 'APPROVED'),
(6, '哈弗', 'H6', '哈弗H6 1.5T Pro', 115900.00, 'SUV', '燃油', 5, 2024, '', 21000, 4.10, 'APPROVED'),
(7, '五菱', '缤果', '缤果 333KM 悦享款', 59800.00, '轿车', '纯电', 4, 2025, '', 16000, 4.10, 'APPROVED'),
(8, '吉利', '银河L7', '银河L7 115KM 星舰版', 138700.00, 'SUV', '插混', 5, 2025, '', 19000, 4.40, 'APPROVED'),
(9, '理想', 'L7', '理想L7 Pro', 319800.00, 'SUV', '增程', 5, 2025, '', 15000, 4.70, 'APPROVED'),
(10, '问界', 'M7', '问界M7 Plus 五座版', 249800.00, 'SUV', '增程', 5, 2025, '', 17000, 4.60, 'APPROVED'),
(11, '别克', 'GL8', 'GL8 陆上公务舱 舒适型', 239900.00, 'MPV', '燃油', 7, 2024, '', 9000, 4.30, 'APPROVED'),
(12, '腾势', 'D9', 'D9 DM-i 970 四驱尊荣型', 339800.00, 'MPV', '插混', 7, 2025, '', 11000, 4.70, 'APPROVED'),
(13, '广汽埃安', 'AION Y', 'AION Y Plus 610 智领版', 119800.00, 'SUV', '纯电', 5, 2025, '', 14000, 4.20, 'APPROVED'),
(14, '小鹏', 'G6', '小鹏G6 580 长续航Pro', 209900.00, 'SUV', '纯电', 5, 2025, '', 12000, 4.40, 'APPROVED'),
(15, '本田', '雅阁', '雅阁 260TURBO 智享版', 179800.00, '轿车', '燃油', 5, 2024, '', 13000, 4.30, 'APPROVED'),
(16, '大众', '帕萨特', '帕萨特 330TSI 精英版', 181900.00, '轿车', '燃油', 5, 2024, '', 12500, 4.20, 'APPROVED'),
(17, '深蓝', 'S7', '深蓝S7 121Max 增程版', 149900.00, 'SUV', '增程', 5, 2025, '', 15500, 4.30, 'APPROVED'),
(18, '吉利', '帝豪', '帝豪 第4代 1.5L CVT豪华型', 69900.00, '轿车', '燃油', 5, 2024, '', 18000, 4.00, 'APPROVED'),
(19, '比亚迪', '海鸥', '海鸥 305KM 活力版', 69800.00, '轿车', '纯电', 4, 2025, '', 26000, 4.30, 'APPROVED'),
(20, '传祺', 'M8', '传祺M8 领秀系列 390T 尊享版', 179800.00, 'MPV', '燃油', 7, 2024, '', 8000, 4.20, 'APPROVED');

INSERT INTO car_param (
    car_id, length_mm, width_mm, height_mm, wheelbase_mm,
    fuel_consumption, electric_consumption, electric_range_km, total_range_km,
    acceleration_100, airbag_count, has_abs, has_esp, has_active_brake,
    has_lane_keep, has_adaptive_cruise, has_blind_spot, has_reverse_camera,
    has_360_camera, has_ota, has_voice_control, has_auto_parking,
    screen_size, assist_drive_level
) VALUES
(1, 4765, 1837, 1495, 2718, 3.8, NULL, 120, 1245, 7.3, 6, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, FALSE, 12.8, 'L2'),
(2, 4775, 1890, 1670, 2765, 4.5, NULL, 110, 1050, 7.9, 6, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, FALSE, 15.6, 'L2'),
(3, 4125, 1770, 1570, 2700, NULL, 11.0, 420, NULL, 10.9, 4, TRUE, TRUE, TRUE, TRUE, TRUE, FALSE, TRUE, TRUE, TRUE, TRUE, FALSE, 12.8, 'L2'),
(4, 4750, 1921, 1624, 2890, NULL, 13.4, 554, NULL, 5.9, 8, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, 15.0, 'L2'),
(5, 4635, 1780, 1435, 2700, 5.7, NULL, NULL, NULL, 12.1, 6, TRUE, TRUE, TRUE, TRUE, TRUE, FALSE, TRUE, FALSE, FALSE, TRUE, FALSE, 10.25, 'L2'),
(6, 4653, 1886, 1730, 2738, 7.1, NULL, NULL, NULL, 9.8, 6, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, FALSE, 12.3, 'L2'),
(7, 3950, 1708, 1580, 2560, NULL, 10.5, 333, NULL, 12.0, 2, TRUE, TRUE, FALSE, FALSE, FALSE, FALSE, TRUE, FALSE, TRUE, TRUE, FALSE, 10.25, 'NONE'),
(8, 4700, 1905, 1685, 2785, 5.2, NULL, 115, 1370, 6.9, 6, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, 13.2, 'L2'),
(9, 5050, 1995, 1750, 3005, 6.8, NULL, 210, 1315, 5.3, 8, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, 15.7, 'L2'),
(10, 5020, 1945, 1775, 2820, 6.9, NULL, 240, 1300, 7.8, 8, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, 15.6, 'L2'),
(11, 5238, 1878, 1776, 3088, 8.1, NULL, NULL, NULL, 9.8, 6, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, FALSE, TRUE, FALSE, 12.0, 'L2'),
(12, 5250, 1960, 1920, 3110, 6.2, NULL, 180, 970, 7.9, 8, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, 15.6, 'L2'),
(13, 4535, 1870, 1650, 2750, NULL, 12.8, 610, NULL, 8.5, 6, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, FALSE, 14.6, 'L2'),
(14, 4753, 1920, 1650, 2890, NULL, 13.2, 580, NULL, 6.6, 6, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, 14.96, 'L2'),
(15, 4980, 1862, 1449, 2830, 6.6, NULL, NULL, NULL, 8.6, 8, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, FALSE, TRUE, TRUE, FALSE, 12.3, 'L2'),
(16, 4948, 1836, 1469, 2871, 6.4, NULL, NULL, NULL, 8.4, 8, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, FALSE, 12.0, 'L2'),
(17, 4750, 1930, 1625, 2900, 5.8, NULL, 121, 1040, 7.6, 6, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, 15.6, 'L2'),
(18, 4638, 1820, 1460, 2650, 6.2, NULL, NULL, NULL, 11.6, 4, TRUE, TRUE, FALSE, FALSE, FALSE, FALSE, TRUE, FALSE, FALSE, TRUE, FALSE, 12.3, 'NONE'),
(19, 3780, 1715, 1540, 2500, NULL, 10.0, 305, NULL, 13.0, 2, TRUE, TRUE, FALSE, FALSE, FALSE, FALSE, TRUE, FALSE, TRUE, TRUE, FALSE, 10.1, 'NONE'),
(20, 5089, 1884, 1822, 3000, 8.8, NULL, NULL, NULL, 9.5, 6, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, FALSE, 14.6, 'L2');
