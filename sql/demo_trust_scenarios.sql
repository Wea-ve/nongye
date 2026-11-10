-- 演示场景数据（幂等，可重复执行）
-- 补齐三类答辩演示缺口：
--   ① 无检测报告批次（可信指数偏低，可对比）
--   ② 订阅到期的信任者（助手"该再买了"提醒有数据）
--   ③ 超时未回复的售后工单（管理员端红色升级角标）
USE nongyeplus;

-- ① 无检测报告批次：黄瓜 BATCH202604001（农户 farmer01，id=2）
INSERT INTO crop_batch (batch_no, farmer_id, crop_name, variety, land_location, plant_date,
                        expect_harvest_date, actual_harvest_date, harvest_weight, status, trace_code,
                        on_sale, price, stock, subscribe_open, promise_summary)
SELECT 'BATCH202604001', 2, '黄瓜', '津优35号', '山东省寿光市洛城街道张家庄村3号大棚',
       DATE_SUB(CURDATE(), INTERVAL 60 DAY), DATE_SUB(CURDATE(), INTERVAL 4 DAY),
       DATE_SUB(CURDATE(), INTERVAL 3 DAY), 420.00, 'HARVESTED', 'TZ202604001002',
       1, 3.80, 420.00, 1, '当日采摘 · 农户承诺不打蜡'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM crop_batch WHERE trace_code = 'TZ202604001002');

-- ② 三位信任者（若已存在则跳过）
INSERT INTO consumer_account (nickname, phone, trust_level, invite_code, inviter_type, inviter_id, last_active_time)
SELECT '王姐', '13800138001', 'FOLLOWER', 'admin', 'admin', 1, NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM consumer_account WHERE phone = '13800138001');

INSERT INTO consumer_account (nickname, phone, trust_level, invite_code, inviter_type, inviter_id, last_active_time)
SELECT '李叔', '13800138002', 'FOLLOWER', 'admin', 'admin', 1, NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM consumer_account WHERE phone = '13800138002');

INSERT INTO consumer_account (nickname, phone, trust_level, invite_code, inviter_type, inviter_id, last_active_time)
SELECT '小陈', '13800138003', 'FOLLOWER', 'admin', 'admin', 1, NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM consumer_account WHERE phone = '13800138003');

-- 关注关系：王姐关注农户、李叔关注批次、小陈两者都关注
INSERT INTO consumer_follow (consumer_id, target_type, target_id, notify_enabled, create_time)
SELECT (SELECT id FROM consumer_account WHERE phone = '13800138001'), 'FARMER', 2, 1, NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM consumer_follow WHERE consumer_id = (SELECT id FROM consumer_account WHERE phone = '13800138001')
                  AND target_type = 'FARMER' AND target_id = 2);

INSERT INTO consumer_follow (consumer_id, target_type, target_id, notify_enabled, create_time)
SELECT (SELECT id FROM consumer_account WHERE phone = '13800138002'), 'BATCH', 1, 1, NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM consumer_follow WHERE consumer_id = (SELECT id FROM consumer_account WHERE phone = '13800138002')
                  AND target_type = 'BATCH' AND target_id = 1);

INSERT INTO consumer_follow (consumer_id, target_type, target_id, notify_enabled, create_time)
SELECT (SELECT id FROM consumer_account WHERE phone = '13800138003'), 'FARMER', 2, 1, NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM consumer_follow WHERE consumer_id = (SELECT id FROM consumer_account WHERE phone = '13800138003')
                  AND target_type = 'FARMER' AND target_id = 2);

INSERT INTO consumer_follow (consumer_id, target_type, target_id, notify_enabled, create_time)
SELECT (SELECT id FROM consumer_account WHERE phone = '13800138003'), 'BATCH', 1, 1, NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM consumer_follow WHERE consumer_id = (SELECT id FROM consumer_account WHERE phone = '13800138003')
                  AND target_type = 'BATCH' AND target_id = 1);

-- 常购订阅：王姐订阅两种（复购），李叔的订阅今天到期（助手提醒），小陈订阅一次
INSERT INTO consumer_subscription (consumer_id, batch_id, crop_name, qty, unit, cycle_days, next_date, status)
SELECT (SELECT id FROM consumer_account WHERE phone = '13800138001'), 1, '西红柿', 5.00, '斤', 7, DATE_ADD(CURDATE(), INTERVAL 2 DAY), 'ACTIVE'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM consumer_subscription
  WHERE consumer_id = (SELECT id FROM consumer_account WHERE phone = '13800138001') AND batch_id = 1);

INSERT INTO consumer_subscription (consumer_id, batch_id, crop_name, qty, unit, cycle_days, next_date, status)
SELECT (SELECT id FROM consumer_account WHERE phone = '13800138001'),
       (SELECT id FROM crop_batch WHERE trace_code = 'TZ202604001002'), '黄瓜', 3.00, '斤', 7, DATE_ADD(CURDATE(), INTERVAL 4 DAY), 'ACTIVE'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM consumer_subscription
  WHERE consumer_id = (SELECT id FROM consumer_account WHERE phone = '13800138001')
    AND batch_id = (SELECT id FROM crop_batch WHERE trace_code = 'TZ202604001002'));

INSERT INTO consumer_subscription (consumer_id, batch_id, crop_name, qty, unit, cycle_days, next_date, status)
SELECT (SELECT id FROM consumer_account WHERE phone = '13800138002'), 1, '西红柿', 2.00, '斤', 7, DATE_SUB(CURDATE(), INTERVAL 1 DAY), 'ACTIVE'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM consumer_subscription
  WHERE consumer_id = (SELECT id FROM consumer_account WHERE phone = '13800138002'));

INSERT INTO consumer_subscription (consumer_id, batch_id, crop_name, qty, unit, cycle_days, next_date, status)
SELECT (SELECT id FROM consumer_account WHERE phone = '13800138003'), 1, '西红柿', 3.00, '斤', 7, DATE_ADD(CURDATE(), INTERVAL 5 DAY), 'ACTIVE'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM consumer_subscription
  WHERE consumer_id = (SELECT id FROM consumer_account WHERE phone = '13800138003'));

-- ③ 超时未回复工单：30 小时前提交，SLA 24 小时，已超时
INSERT INTO complaint (batch_id, consumer_id, complainant, phone, query_code, type, content,
                       status, sla_hours, create_time, first_reply_time, overdue)
SELECT 1, (SELECT id FROM consumer_account WHERE phone = '13800138001'), '王姐', '13800138001',
       'OVERTIME2026', 'QUALITY', '收到的西红柿有两颗已经软烂，希望补发或退款', 'PENDING', 24,
       DATE_SUB(NOW(), INTERVAL 30 HOUR), NULL, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM complaint WHERE query_code = 'OVERTIME2026');

-- 已闭环工单：用于计算"售后闭环时长"（首响 5 小时）
INSERT INTO complaint (batch_id, consumer_id, complainant, phone, query_code, type, content,
                       status, sla_hours, create_time, reply, reply_time, first_reply_time)
SELECT 1, (SELECT id FROM consumer_account WHERE phone = '13800138002'), '李叔', '13800138002',
       'CLOSED2026', 'FRESHNESS', '到货时蔬菜有点蔫，反馈一下', 'CLOSED', 24,
       DATE_SUB(NOW(), INTERVAL 3 DAY), '已为您登记并补偿，下次配送优先安排新采批次', DATE_SUB(NOW(), INTERVAL 3 DAY) + INTERVAL 5 HOUR,
       DATE_SUB(NOW(), INTERVAL 3 DAY) + INTERVAL 5 HOUR
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM complaint WHERE query_code = 'CLOSED2026');
