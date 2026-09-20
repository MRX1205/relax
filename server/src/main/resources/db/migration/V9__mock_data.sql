-- ============================================================
-- Mock data for development/demo
-- ============================================================

-- 1. Service categories
INSERT INTO service_category (id, name, sort, status) VALUES
(101, '中医推拿', 1, 'ENABLED'),
(102, '精油SPA', 2, 'ENABLED'),
(103, '足疗保健', 3, 'ENABLED'),
(104, '运动康复', 4, 'ENABLED');

-- 2. Service projects
INSERT INTO service_project (id, category_id, name, duration_minutes, base_price, description, notice, status, sort) VALUES
(201, 101, '全身推拿60分钟', 60, 198.00, '传统中医推拿，疏通经络，缓解肌肉疲劳。适合久坐办公、运动后恢复。', '请提前10分钟到达，穿着宽松衣物。', 'ON_SHELF', 1),
(202, 101, '肩颈专项45分钟', 45, 158.00, '针对肩颈部位深度按摩，缓解颈椎不适和肩部僵硬。', '如有颈椎病史请提前告知。', 'ON_SHELF', 2),
(203, 102, '全身精油SPA90分钟', 90, 388.00, '进口精油配合专业手法，全身放松，改善睡眠质量。', '请提前沐浴，保持皮肤清洁。', 'ON_SHELF', 1),
(204, 102, '背部精油60分钟', 60, 268.00, '背部深层精油按摩，缓解背部紧张和压力。', '适合工作压力大的人群。', 'ON_SHELF', 2),
(205, 103, '经典足疗60分钟', 60, 168.00, '足底反射区按摩，促进血液循环，改善睡眠。', '请提前泡脚，效果更佳。', 'ON_SHELF', 1),
(206, 103, '足疗+全身90分钟', 90, 258.00, '足疗加全身放松套餐，双重享受。', '套餐时间较长，请预留充足时间。', 'ON_SHELF', 2),
(207, 104, '运动恢复75分钟', 75, 298.00, '针对运动后肌肉酸痛的专业恢复按摩。', '适合健身、跑步等运动爱好者。', 'ON_SHELF', 1);

-- 3. Create demo technician user (if not exists from login)
-- The bootstrap super admin user is created on first login
-- We'll add technician profile for them after they log in

-- 4. Create additional demo users
INSERT INTO platform_user (id, wechat_open_id, nickname, avatar_url, phone, status, last_role) VALUES
(9001, 'mock:demo-user-001', '张小明', NULL, '13800001111', 'ACTIVE', 'USER'),
(9002, 'mock:demo-user-002', '李美丽', NULL, '13800002222', 'ACTIVE', 'USER'),
(9003, 'mock:demo-tech-001', '王师傅', NULL, '13800003333', 'ACTIVE', 'TECHNICIAN'),
(9004, 'mock:demo-tech-002', '陈技师', NULL, '13800004444', 'ACTIVE', 'TECHNICIAN'),
(9005, 'mock:demo-tech-003', '刘按摩师', NULL, '13800005555', 'ACTIVE', 'TECHNICIAN');

-- Grant roles
INSERT INTO iam_user_role (user_id, role_id, status) VALUES
(9001, 1, 'ENABLED'),
(9002, 1, 'ENABLED'),
(9003, 1, 'ENABLED'),
(9003, 2, 'ENABLED'),
(9004, 1, 'ENABLED'),
(9004, 2, 'ENABLED'),
(9005, 1, 'ENABLED'),
(9005, 2, 'ENABLED');

-- 5. Technician profiles
INSERT INTO technician (id, user_id, service_name, real_name, phone, intro, experience_years, status, online_status) VALUES
(3001, 9003, '王师傅·中式推拿', '王大力', '13800003333', '10年中医推拿经验，擅长全身推拿和肩颈调理。曾在多家知名养生馆工作，手法娴熟，深受顾客好评。', 10, 'ACTIVE', 'ONLINE'),
(3002, 9004, '陈技师·精油SPA', '陈雅婷', '13800004444', '8年精油SPA经验，精通各种精油配方。擅长全身放松和背部深层按摩，手法细腻温柔。', 8, 'ACTIVE', 'ONLINE'),
(3003, 9005, '刘师傅·运动康复', '刘强', '13800005555', '12年运动康复经验，前专业运动员。擅长运动后肌肉恢复和足疗保健。', 12, 'ACTIVE', 'OFFLINE');

-- 6. Technician projects (pricing)
INSERT INTO technician_project (id, technician_id, project_id, override_price, status) VALUES
(4001, 3001, 201, 188.00, 'ENABLED'),
(4002, 3001, 202, 148.00, 'ENABLED'),
(4003, 3001, 205, 158.00, 'ENABLED'),
(4004, 3002, 203, 368.00, 'ENABLED'),
(4005, 3002, 204, 258.00, 'ENABLED'),
(4006, 3003, 207, 288.00, 'ENABLED'),
(4007, 3003, 201, 198.00, 'ENABLED'),
(4008, 3003, 205, 168.00, 'ENABLED'),
(4009, 3003, 206, 248.00, 'ENABLED');

-- 7. Technician schedules (next 7 days)
INSERT INTO technician_schedule (id, technician_id, schedule_date, start_time, end_time, type, status) VALUES
(5001, 3001, CURRENT_DATE, '09:00', '12:00', 'AVAILABLE', 'ENABLED'),
(5002, 3001, CURRENT_DATE, '14:00', '18:00', 'AVAILABLE', 'ENABLED'),
(5003, 3001, CURRENT_DATE + 1, '09:00', '18:00', 'AVAILABLE', 'ENABLED'),
(5004, 3001, CURRENT_DATE + 2, '09:00', '12:00', 'AVAILABLE', 'ENABLED'),
(5005, 3001, CURRENT_DATE + 2, '14:00', '18:00', 'AVAILABLE', 'ENABLED'),
(5006, 3002, CURRENT_DATE, '10:00', '18:00', 'AVAILABLE', 'ENABLED'),
(5007, 3002, CURRENT_DATE + 1, '09:00', '12:00', 'AVAILABLE', 'ENABLED'),
(5008, 3002, CURRENT_DATE + 1, '14:00', '18:00', 'AVAILABLE', 'ENABLED'),
(5009, 3002, CURRENT_DATE + 3, '09:00', '18:00', 'AVAILABLE', 'ENABLED'),
(5010, 3003, CURRENT_DATE, '09:00', '18:00', 'AVAILABLE', 'ENABLED'),
(5011, 3003, CURRENT_DATE + 1, '09:00', '18:00', 'AVAILABLE', 'ENABLED'),
(5012, 3003, CURRENT_DATE + 2, '09:00', '18:00', 'AVAILABLE', 'ENABLED'),
(5013, 3003, CURRENT_DATE + 3, '09:00', '18:00', 'AVAILABLE', 'ENABLED'),
(5014, 3003, CURRENT_DATE + 4, '09:00', '18:00', 'AVAILABLE', 'ENABLED');

-- 8. Sample orders (various statuses)
INSERT INTO service_order (id, order_no, user_id, technician_id, project_id, status, service_date, start_time, end_time, version, note) VALUES
(6001, 'ORD20260909001', 9001, 3001, 201, 'COMPLETED', CURRENT_DATE - 3, '10:00', '11:00', 5, '第一次体验'),
(6002, 'ORD20260909002', 9002, 3002, 203, 'COMPLETED', CURRENT_DATE - 2, '14:00', '15:30', 5, NULL),
(6003, 'ORD20260909003', 9001, 3003, 207, 'IN_SERVICE', CURRENT_DATE, '09:30', '10:45', 3, '运动后需要放松'),
(6004, 'ORD20260909004', 9002, 3001, 202, 'PAID', CURRENT_DATE + 1, '10:00', '10:45', 1, NULL),
(6005, 'ORD20260909005', 9001, 3002, 204, 'ACCEPTED', CURRENT_DATE + 1, '14:00', '15:00', 1, '肩颈不太好'),
(6006, 'ORD20260909006', 9002, 3001, 205, 'PENDING_PAYMENT', CURRENT_DATE + 2, '09:00', '10:00', 0, NULL),
(6007, 'ORD20260909007', 9001, 3003, 201, 'CANCELLED', CURRENT_DATE - 1, '15:00', '16:00', 2, '临时有事');

-- Order snapshots
INSERT INTO order_project_snapshot (order_id, project_name, duration_minutes, base_price, override_price, actual_price) VALUES
(6001, '全身推拿60分钟', 60, 198.00, 188.00, 188.00),
(6002, '全身精油SPA90分钟', 90, 388.00, 368.00, 368.00),
(6003, '运动恢复75分钟', 75, 298.00, 288.00, 288.00),
(6004, '肩颈专项45分钟', 45, 158.00, 148.00, 148.00),
(6005, '背部精油60分钟', 60, 268.00, 258.00, 258.00),
(6006, '经典足疗60分钟', 60, 168.00, 158.00, 158.00),
(6007, '全身推拿60分钟', 60, 198.00, 198.00, 198.00);

INSERT INTO order_address_snapshot (order_id, contact_name, contact_phone, region_name, detail, longitude, latitude) VALUES
(6001, '张小明', '13800001111', '南城街道', '鸿福路100号A座1201', 113.750000, 23.020000),
(6002, '李美丽', '13800002222', '东城街道', '东城中路88号碧桂园3栋502', 113.780000, 23.030000),
(6003, '张小明', '13800001111', '南城街道', '鸿福路100号A座1201', 113.750000, 23.020000),
(6004, '李美丽', '13800002222', '东城街道', '东城中路88号碧桂园3栋502', 113.780000, 23.030000),
(6005, '张小明', '13800001111', '南城街道', '鸿福路100号A座1201', 113.750000, 23.020000),
(6006, '李美丽', '13800002222', '东城街道', '东城中路88号碧桂园3栋502', 113.780000, 23.030000),
(6007, '张小明', '13800001111', '南城街道', '鸿福路100号A座1201', 113.750000, 23.020000);

INSERT INTO order_amount (order_id, project_amount, travel_fee, discount_amount, payable_amount, paid_amount, refunded_amount) VALUES
(6001, 188.00, 20.00, 0.00, 208.00, 208.00, 0.00),
(6002, 368.00, 20.00, 0.00, 388.00, 388.00, 0.00),
(6003, 288.00, 20.00, 0.00, 308.00, 308.00, 0.00),
(6004, 148.00, 20.00, 0.00, 168.00, 168.00, 0.00),
(6005, 258.00, 20.00, 0.00, 278.00, 278.00, 0.00),
(6006, 158.00, 20.00, 0.00, 178.00, 0.00, 0.00),
(6007, 198.00, 20.00, 0.00, 218.00, 0.00, 0.00);

-- Order status logs
INSERT INTO order_status_log (id, order_id, from_status, to_status, operator_type, operator_id, reason) VALUES
(7001, 6001, NULL, 'PENDING_PAYMENT', 'SYSTEM', NULL, '订单创建'),
(7002, 6001, 'PENDING_PAYMENT', 'PAID', 'SYSTEM', NULL, '支付成功'),
(7003, 6001, 'PAID', 'ACCEPTED', 'TECHNICIAN', 9003, '技师接单'),
(7004, 6001, 'ACCEPTED', 'TECHNICIAN_DEPARTING', 'TECHNICIAN', 9003, '技师出发'),
(7005, 6001, 'TECHNICIAN_DEPARTING', 'TECHNICIAN_ARRIVED', 'TECHNICIAN', 9003, '技师到达'),
(7006, 6001, 'TECHNICIAN_ARRIVED', 'IN_SERVICE', 'TECHNICIAN', 9003, '开始服务'),
(7007, 6001, 'IN_SERVICE', 'COMPLETED', 'TECHNICIAN', 9003, '服务完成'),
(7008, 6002, NULL, 'PENDING_PAYMENT', 'SYSTEM', NULL, '订单创建'),
(7009, 6002, 'PENDING_PAYMENT', 'PAID', 'SYSTEM', NULL, '支付成功'),
(7010, 6002, 'PAID', 'ACCEPTED', 'TECHNICIAN', 9004, '技师接单'),
(7011, 6002, 'ACCEPTED', 'COMPLETED', 'TECHNICIAN', 9004, '服务完成'),
(7012, 6003, NULL, 'PENDING_PAYMENT', 'SYSTEM', NULL, '订单创建'),
(7013, 6003, 'PENDING_PAYMENT', 'PAID', 'SYSTEM', NULL, '支付成功'),
(7014, 6003, 'PAID', 'ACCEPTED', 'TECHNICIAN', 9005, '技师接单'),
(7015, 6003, 'ACCEPTED', 'IN_SERVICE', 'TECHNICIAN', 9005, '开始服务'),
(7016, 6004, NULL, 'PENDING_PAYMENT', 'SYSTEM', NULL, '订单创建'),
(7017, 6004, 'PENDING_PAYMENT', 'PAID', 'SYSTEM', NULL, '支付成功'),
(7018, 6005, NULL, 'PENDING_PAYMENT', 'SYSTEM', NULL, '订单创建'),
(7019, 6005, 'PENDING_PAYMENT', 'PAID', 'SYSTEM', NULL, '支付成功'),
(7020, 6005, 'PAID', 'ACCEPTED', 'TECHNICIAN', 9004, '技师接单'),
(7021, 6006, NULL, 'PENDING_PAYMENT', 'SYSTEM', NULL, '订单创建'),
(7022, 6007, NULL, 'PENDING_PAYMENT', 'SYSTEM', NULL, '订单创建'),
(7023, 6007, 'PENDING_PAYMENT', 'CANCELLED', 'USER', 9001, '临时有事取消');

-- 9. Reviews for completed orders
INSERT INTO review (id, order_id, user_id, technician_id, score, content, status) VALUES
(8001, 6001, 9001, 3001, 5, '王师傅手法很好，按完肩颈舒服多了！下次还会预约。', 'VISIBLE'),
(8002, 6002, 9002, 3002, 4, '精油味道很好，全身放松。环境也很干净。', 'ON_SHELF');

-- 10. Banners
INSERT INTO banner (id, title, image_file_id, link_type, link_value, sort, status) VALUES
(901, '新用户首单立减20元', NULL, 'PROJECT', '201', 1, 'ENABLED'),
(902, '精油SPA特惠周', NULL, 'CATEGORY', '102', 2, 'ENABLED'),
(903, '技师招募中', NULL, NULL, NULL, 3, 'ENABLED');

-- 11. Technician photos (placeholder references)
INSERT INTO technician_photo (id, technician_id, file_id, photo_type, sort, audit_status) VALUES
(10001, 3001, 0, 'PORTRAIT', 1, 'APPROVED'),
(10002, 3001, 0, 'WORK', 2, 'APPROVED'),
(10003, 3002, 0, 'PORTRAIT', 1, 'APPROVED'),
(10004, 3003, 0, 'PORTRAIT', 1, 'APPROVED');
