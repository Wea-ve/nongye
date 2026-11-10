-- 信任承诺种子数据（幂等，可重复执行）
-- 给演示批次 1（西红柿 TZ202603001001，农户 id=2）预置三条事前承诺
USE nongyeplus;

INSERT INTO trust_promise (batch_id, farmer_id, promise_type, promise_value, evidence_path, status, create_time)
SELECT 1, 2, 'FRESH', '24h', '采收当日 9:00 采摘，同城冷链当日入库', 'ACTIVE', NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM trust_promise WHERE batch_id = 1 AND promise_type = 'FRESH');

INSERT INTO trust_promise (batch_id, farmer_id, promise_type, promise_value, evidence_path, status, create_time)
SELECT 1, 2, 'HARVEST_DATE', '2026-06-15', '按约定采收日前后 3 天内采收', 'ACTIVE', NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM trust_promise WHERE batch_id = 1 AND promise_type = 'HARVEST_DATE');

INSERT INTO trust_promise (batch_id, farmer_id, promise_type, promise_value, evidence_path, status, create_time)
SELECT 1, 2, 'QUALITY', '无农残、不打蜡，已通过第三方检测', '检测报告编号见检测报告页', 'ACTIVE', NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM trust_promise WHERE batch_id = 1 AND promise_type = 'QUALITY');
