-- 消费场景知识库补充（幂等，可重复执行）
-- 目的：补齐消费者真正会问的三类问题——怎么存、能不能吃、怎么洗，
-- 让 RAG 检索在"这菜怎么存""孕妇能吃吗""怎么洗"这类问题上能命中。
USE nongyeplus;

INSERT INTO knowledge_doc (title, category, content, source, doc_version)
SELECT * FROM (SELECT
  '西红柿买回家怎么保存' AS title, '消费科普' AS category,
  '番茄未完全成熟时可常温阴凉处放置 3-5 天催熟；成熟后如需延缓变软，可放入冰箱冷藏 3-5 天，但冷藏会降低风味。切开后必须密封冷藏并在 1-2 天内吃完。避免与黄瓜同放，番茄释放的乙烯会加速黄瓜变软。' AS content,
  '中国居民膳食指南（2022）· 食物储存建议' AS source, 'v1.0' AS doc_version
) t WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '西红柿买回家怎么保存');

INSERT INTO knowledge_doc (title, category, content, source, doc_version)
SELECT * FROM (SELECT
  '叶菜类蔬菜怎么保鲜', '消费科普',
  '菠菜、生菜、油麦菜等叶菜买回后不要先水洗，用厨房纸包裹后装入保鲜袋，排出多余空气，冷藏可保存 2-3 天。水洗后叶面残留水分会加速腐烂。如已清洗，需沥干并用密封盒冷藏，24 小时内食用。',
  '中国居民膳食指南（2022）· 食物储存建议', 'v1.0'
) t WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '叶菜类蔬菜怎么保鲜');

INSERT INTO knowledge_doc (title, category, content, source, doc_version)
SELECT * FROM (SELECT
  '黄瓜怎么存放更久', '消费科普',
  '黄瓜用保鲜袋包好放冰箱冷藏室，可保存 3-5 天，适宜温度 10-12℃。不要与番茄、香蕉等释放乙烯的水果同放，否则易发黄变软。表面有刺的黄瓜更耐放，切口处易腐烂应优先食用。',
  '中国居民膳食指南（2022）· 食物储存建议', 'v1.0'
) t WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '黄瓜怎么存放更久');

INSERT INTO knowledge_doc (title, category, content, source, doc_version)
SELECT * FROM (SELECT
  '蔬菜怎么洗才能去除农药残留', '消费科普',
  '推荐流程：先摘去烂叶，用流动清水冲洗 30 秒以上；再用清水浸泡 10-15 分钟（不要超过 30 分钟，避免营养流失与农残回渗）；最后再冲洗一遍。叶菜可逐叶掰开清洗，瓜果类可用软刷轻刷表面。不建议使用洗洁精清洗蔬菜。',
  'GB 2763-2021 食品中农药最大残留限量 · 配套科普', 'v1.0'
) t WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '蔬菜怎么洗才能去除农药残留');

INSERT INTO knowledge_doc (title, category, content, source, doc_version)
SELECT * FROM (SELECT
  '孕妇能吃这些蔬菜吗', '消费科普',
  '孕妇可以正常食用经过检测合格的蔬菜，重点注意三点：一是生食蔬菜（如生菜、紫甘蓝）需彻底清洗，降低弓形虫与致病菌风险；二是避免食用未煮熟的豆角、发芽土豆；三是优先选择有第三方检测报告、农残合格的批次蔬菜。如有特殊医嘱以医生意见为准。',
  '中国孕期妇女膳食指南（2022）', 'v1.0'
) t WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '孕妇能吃这些蔬菜吗');

INSERT INTO knowledge_doc (title, category, content, source, doc_version)
SELECT * FROM (SELECT
  '宝宝辅食蔬菜怎么选怎么处理', '消费科普',
  '婴幼儿辅食蔬菜应选择新鲜、无腐烂、有检测合格报告的批次。制作前用流动水充分清洗并去皮，蒸煮熟透后打成泥。1 岁以内不加盐糖。现做现吃，剩余辅食冷藏不超过 24 小时，再次食用需彻底加热。',
  '中国居民膳食指南（2022）· 婴幼儿喂养', 'v1.0'
) t WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '宝宝辅食蔬菜怎么选怎么处理');

INSERT INTO knowledge_doc (title, category, content, source, doc_version)
SELECT * FROM (SELECT
  '隔夜菜还能不能吃', '消费科普',
  '绿叶菜不建议隔夜：反复加热会使亚硝酸盐升高，建议当餐吃完。瓜果类、根茎类菜肴冷藏保存 24 小时内、彻底加热后可以食用。剩菜应在出锅后 2 小时内放入冰箱，不要在室温放置过久；再次食用必须加热至中心温度 70℃ 以上。',
  '中国居民膳食指南（2022）· 食品安全', 'v1.0'
) t WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '隔夜菜还能不能吃');

INSERT INTO knowledge_doc (title, category, content, source, doc_version)
SELECT * FROM (SELECT
  '什么是农药安全间隔期', '消费科普',
  '安全间隔期是指最后一次施药到作物采收之间必须间隔的最短天数，目的是让农药降解到国家标准允许的残留量以下。不同农药、不同作物的安全间隔期不同，常见为 3-14 天。购买时可关注批次最后一次用药日期与采收日期的间隔是否满足要求。',
  'GB 2763-2021 食品中农药最大残留限量', 'v1.0'
) t WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '什么是农药安全间隔期');

INSERT INTO knowledge_doc (title, category, content, source, doc_version)
SELECT * FROM (SELECT
  '检测报告怎么看是否合格', '消费科普',
  '看三处：一是检测机构是否具备 CMA/CNAS 资质；二是检测项目是否覆盖常见农残（如毒死蜱、克百威、氧乐果等）；三是结论是否为"合格"以及检测日期是否在保质期内。单份报告只对送检批次负责，不能代表所有批次。建议结合溯源信息一起判断。',
  'GB 2763-2021 食品中农药最大残留限量', 'v1.0'
) t WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '检测报告怎么看是否合格');

INSERT INTO knowledge_doc (title, category, content, source, doc_version)
SELECT * FROM (SELECT
  '无公害、绿色、有机有什么区别', '消费科普',
  '无公害农产品是市场准入基本要求，允许限量使用化学农药；绿色食品分 A 级和 AA 级，A 级允许限量使用，AA 级接近有机；有机产品禁止使用化学合成农药和化肥，认证最严格。三者认证标准与检测要求依次提高，购买时以包装上的认证标志和证书编号为准。',
  'NY/T 393 绿色食品 农药使用准则', 'v1.0'
) t WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '无公害、绿色、有机有什么区别');

INSERT INTO knowledge_doc (title, category, content, source, doc_version)
SELECT * FROM (SELECT
  '溯源码能查到哪些信息', '消费科普',
  '一个合格的溯源码通常可查：产地与农户、播种与采收日期、农事记录（施肥、用药、灌溉）、第三方检测报告、以及采收后的流转链路（入库、出库、经销商）。信息越完整、节点越连续，可信度越高。仅有产地和名称的溯源页，参考价值有限。',
  'GS1 农产品追溯通用规范', 'v1.0'
) t WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '溯源码能查到哪些信息');

INSERT INTO knowledge_doc (title, category, content, source, doc_version)
SELECT * FROM (SELECT
  '蔬菜生吃有哪些风险', '消费科普',
  '生吃蔬菜主要风险为致病菌（沙门氏菌、大肠杆菌）与寄生虫卵。建议：生食前用流动水充分清洗，必要时用食品级消毒液按说明处理；免疫力较低人群（孕妇、老人、幼儿）尽量避免生食叶菜；生熟砧板刀具分开使用。',
  '中国居民膳食指南（2022）· 食品安全', 'v1.0'
) t WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '蔬菜生吃有哪些风险');

INSERT INTO knowledge_doc (title, category, content, source, doc_version)
SELECT * FROM (SELECT
  '冰箱冷藏室温度设多少合适', '消费科普',
  '家用冰箱冷藏室建议设为 4℃ 左右（0-4℃ 区间），冷冻室 -18℃ 以下。蔬菜存放于冷藏室下层抽屉，避免与生肉同层以防交叉污染。冰箱不宜塞得过满，需留出冷气循环空间；建议每季度清洁一次。',
  '中国居民膳食指南（2022）· 食物储存建议', 'v1.0'
) t WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '冰箱冷藏室温度设多少合适');

INSERT INTO knowledge_doc (title, category, content, source, doc_version)
SELECT * FROM (SELECT
  '蔬菜冷藏前要不要洗', '消费科普',
  '多数蔬菜冷藏前不建议水洗。表面水分会成为微生物繁殖的温床，加速腐烂。正确做法是带泥存放、食用前再清洗。土豆、洋葱、南瓜等根茎瓜类适合常温阴凉通风存放，不必进冰箱。',
  '中国居民膳食指南（2022）· 食物储存建议', 'v1.0'
) t WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '蔬菜冷藏前要不要洗');

INSERT INTO knowledge_doc (title, category, content, source, doc_version)
SELECT * FROM (SELECT
  '哪些蔬菜不适合放冰箱', '消费科普',
  '土豆、红薯、洋葱、大蒜、南瓜、冬瓜等适合 10-15℃ 阴凉通风处存放，低温会使其淀粉转化、口感变差或加速腐烂。番茄、黄瓜、茄子等冷敏蔬菜长时间低于 10℃ 会出现冷害斑点，建议短期常温存放。',
  '中国居民膳食指南（2022）· 食物储存建议', 'v1.0'
) t WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '哪些蔬菜不适合放冰箱');

INSERT INTO knowledge_doc (title, category, content, source, doc_version)
SELECT * FROM (SELECT
  '如何判断蔬菜是否新鲜', '消费科普',
  '看四点：一看色泽是否自然鲜亮、有无异常斑点；二看叶片/表皮是否挺括，萎蔫说明失水；三闻气味有无酸腐或刺鼻异味；四摸质地是否紧实，发黏说明已变质。购买有溯源的批次时，可对照采收日期判断在途天数。',
  '中国居民膳食指南（2022）· 食物选购', 'v1.0'
) t WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '如何判断蔬菜是否新鲜');

INSERT INTO knowledge_doc (title, category, content, source, doc_version)
SELECT * FROM (SELECT
  '反季节蔬菜能不能吃', '消费科普',
  '反季节蔬菜在规范种植与检测合格的前提下可以正常食用，营养价值与应季蔬菜差异不大。关注重点应是农残检测是否合格、储运是否规范，而非是否应季。设施栽培的蔬菜同样受 GB 2763 农残限量标准约束。',
  'GB 2763-2021 食品中农药最大残留限量', 'v1.0'
) t WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '反季节蔬菜能不能吃');

INSERT INTO knowledge_doc (title, category, content, source, doc_version)
SELECT * FROM (SELECT
  '蔬菜焯水有什么好处', '消费科普',
  '焯水可去除部分农残、草酸和涩味，缩短后续烹饪时间从而保留更多维生素。建议水沸后下锅，叶菜 30 秒-1 分钟、根茎类 1-2 分钟，捞出后可过凉水保持色泽。焯菜水建议倒掉，不要用来做汤。',
  '中国居民膳食指南（2022）· 烹调建议', 'v1.0'
) t WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '蔬菜焯水有什么好处');

INSERT INTO knowledge_doc (title, category, content, source, doc_version)
SELECT * FROM (SELECT
  '发芽的土豆能不能吃', '消费科普',
  '不能。土豆发芽或表皮变绿时龙葵素含量显著升高，普通烹调难以破坏，食用后可能引起恶心、呕吐、腹泻等中毒症状。轻微发芽可深挖芽眼及周围组织并去皮后谨慎食用，明显发芽或大面积变绿应直接丢弃。',
  '中国居民膳食指南（2022）· 食品安全', 'v1.0'
) t WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '发芽的土豆能不能吃');

INSERT INTO knowledge_doc (title, category, content, source, doc_version)
SELECT * FROM (SELECT
  '豆角为什么要彻底煮熟', '消费科普',
  '四季豆、扁豆等豆角含皂苷和植物血凝素，未煮熟可引起食物中毒，表现为恶心、呕吐、腹痛、腹泻。必须加热至完全变色、无生绿色、无豆腥味，建议先焯水再炒制，确保彻底熟透。',
  '中国居民膳食指南（2022）· 食品安全', 'v1.0'
) t WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '豆角为什么要彻底煮熟');

INSERT INTO knowledge_doc (title, category, content, source, doc_version)
SELECT * FROM (SELECT
  '买菜时怎么快速判断批次是否可追溯', '消费科普',
  '扫码后先看四件事：是否有产地与农户实名信息、是否有采收日期、是否有第三方检测报告及结论、是否有完整的流转链路。四项齐全且时间连续，说明批次可追溯性较好；若只有产地名称而无检测与流转记录，建议向商家进一步确认。',
  'GS1 农产品追溯通用规范', 'v1.0'
) t WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '买菜时怎么快速判断批次是否可追溯');

INSERT INTO knowledge_doc (title, category, content, source, doc_version)
SELECT * FROM (SELECT
  '农产品出现异味或变质怎么维权', '消费科普',
  '保留购物凭证、包装与问题商品照片，记录购买时间与批次溯源码；先向商家或平台协商退换；协商不成可通过 12315 平台投诉，或向当地市场监管部门举报。有溯源码的批次可凭码快速定位产地与流转环节，便于追溯责任。',
  '消费者权益保护法（2013 修正）', 'v1.0'
) t WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '农产品出现异味或变质怎么维权');

INSERT INTO knowledge_doc (title, category, content, source, doc_version)
SELECT * FROM (SELECT
  '冷链运输对蔬菜品质的影响', '消费科普',
  '叶菜类采收后呼吸作用强，常温下每多放置 1 小时品质下降明显。规范冷链要求在采收后尽快预冷至 0-4℃，全程冷链运输。承诺"24 小时入库"的批次，可通过溯源链路中采收时间与入库时间之差直接验证是否做到。',
  'GB 31605-2020 食品安全国家标准 食品冷链物流卫生规范', 'v1.0'
) t WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '冷链运输对蔬菜品质的影响');

INSERT INTO knowledge_doc (title, category, content, source, doc_version)
SELECT * FROM (SELECT
  '每天应该吃多少蔬菜', '消费科普',
  '中国居民膳食指南建议成年人每天摄入蔬菜 300-500 克，其中深色蔬菜应占一半以上。蔬菜提供膳食纤维、维生素 C、叶酸和钾等营养素，建议餐餐有蔬菜，品种多样化，不要用果汁代替新鲜蔬菜。',
  '中国居民膳食指南（2022）· 平衡膳食准则', 'v1.0'
) t WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '每天应该吃多少蔬菜');
