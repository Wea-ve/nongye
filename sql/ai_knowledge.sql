-- =============================================
-- AI 深化升级：农事知识库表 + 种子数据
-- 数据库：nongyeplus
-- 说明：
--   1. 供 AI 助手 RAG 检索使用（向量化后做余弦相似度匹配）
--   2. embedding 字段由应用启动后首次检索时自动计算回写，无需手工维护
--   3. 脚本可重复执行：建表用 IF NOT EXISTS，插入按 title 去重
-- =============================================

USE nongyeplus;

SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 农事知识库文档表
-- ----------------------------
CREATE TABLE IF NOT EXISTS knowledge_doc (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    title       VARCHAR(200) NOT NULL COMMENT '知识标题',
    category    VARCHAR(50)  COMMENT '分类：法规标准/种植标准/储存保鲜/检测知识/流通标准/消费科普',
    content     TEXT         NOT NULL COMMENT '知识正文（喂给大模型的片段）',
    source      VARCHAR(200) COMMENT '依据来源（标准号/发布机构/平台整理）',
    doc_version VARCHAR(50)  COMMENT '知识版本（标准版本或整理月份）',
    embedding   MEDIUMTEXT   COMMENT '向量（JSON 数组，text-embedding-v3 生成，应用自动回写）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_knowledge_category (category)
) COMMENT '农事知识库（AI RAG 检索）';

SET FOREIGN_KEY_CHECKS = 1;

-- ----------------------------
-- 种子知识（按 title 去重，可重复执行）
-- ----------------------------
INSERT INTO knowledge_doc (title, category, content)
SELECT * FROM (SELECT 'GB2763 叶菜类蔬菜农残限量常识', '法规标准',
'根据国家标准 GB 2763《食品中农药最大残留限量》，叶菜类蔬菜（如小白菜、生菜、油麦菜）对常见杀虫剂的限量要求严格：毒死蜱最大残留限量 0.1 mg/kg，氯氰菊酯 2 mg/kg，吡虫啉 1 mg/kg。检测报告结果低于该限值即判定为合格。') AS t
WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = 'GB2763 叶菜类蔬菜农残限量常识');

INSERT INTO knowledge_doc (title, category, content)
SELECT * FROM (SELECT 'GB2763 番茄黄瓜农残限量常识', '法规标准',
'根据 GB 2763 标准，番茄（西红柿）中毒死蜱限量为 0.02 mg/kg、氯氰菊酯 0.5 mg/kg；黄瓜中毒死蜱 0.02 mg/kg、多菌灵 2 mg/kg。批次检测报告数值低于限量即为合格产品，可放心采购。') AS t
WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = 'GB2763 番茄黄瓜农残限量常识');

INSERT INTO knowledge_doc (title, category, content)
SELECT * FROM (SELECT '农药安全间隔期常识', '种植标准',
'安全间隔期指最后一次施药到采收的最短间隔天数。常见规定：毒死蜱 7 天，氯氰菊酯 3 天，吡虫啉 5 天，代森锰锌 10 天。合规农户会在采收前按标签停药；消费者可通过农事记录中的打药日期与采收日期差值判断是否满足间隔期。') AS t
WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '农药安全间隔期常识');

INSERT INTO knowledge_doc (title, category, content)
SELECT * FROM (SELECT '绿色种植用药规范', '种植标准',
'绿色食品种植鼓励优先采用农业防治、物理防治（防虫网、粘虫板、杀虫灯）和生物防治（苏云金杆菌、苦参碱等生物农药），化学农药应选用低毒低残留品种并严格控制用量与次数，禁止使用国家明令禁止的高毒农药如甲拌磷、克百威、氧乐果。') AS t
WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '绿色种植用药规范');

INSERT INTO knowledge_doc (title, category, content)
SELECT * FROM (SELECT '西红柿采后储存方法', '储存保鲜',
'西红柿适宜储存温度 10-13 摄氏度，未成熟的青果可常温放置待后熟，成熟红果冷藏可保存 5-7 天。注意：冷藏温度过低（低于 5 度）会造成冷害、风味变差；切开后的西红柿需密封冷藏并在 24 小时内食用。') AS t
WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '西红柿采后储存方法');

INSERT INTO knowledge_doc (title, category, content)
SELECT * FROM (SELECT '叶菜类储存保鲜方法', '储存保鲜',
'小白菜、生菜等叶菜呼吸旺盛，建议用厨房纸包裹后装入保鲜袋，冷藏 0-4 摄氏度可存放 3-5 天；切勿与苹果、香蕉同放，乙烯气体会加速叶片黄化。购买后尽早食用，久放营养流失。') AS t
WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '叶菜类储存保鲜方法');

INSERT INTO knowledge_doc (title, category, content)
SELECT * FROM (SELECT '农残检测流程与合格判定', '检测知识',
'农产品农残检测由第三方机构按 GB 23200 标准流程执行：样品前处理（取样、粉碎、提取）→ 气相/液相色谱-质谱联用仪定量分析 → 对照 GB 2763 限量判定。报告包含检测机构资质（CMA 章）、样品编号、检测项目与结论，数值低于限量即合格。') AS t
WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '农残检测流程与合格判定');

INSERT INTO knowledge_doc (title, category, content)
SELECT * FROM (SELECT '农产品冷链流转要求', '流通标准',
'新鲜果蔬从产地到销售终端建议全程冷链：预冷（采收 2 小时内降至储运温度）→ 冷藏运输（叶菜 0-4 度，茄果类 8-12 度）→ 冷藏销售柜陈列。常温长时间运输会导致失水萎蔫与品质下降，经销商在流转记录中登记各环节温度可提升溯源可信度。') AS t
WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '农产品冷链流转要求');

INSERT INTO knowledge_doc (title, category, content)
SELECT * FROM (SELECT '消费者清洗蔬果去农残技巧', '消费科普',
'家庭去除表面农残的有效方法：1. 流水冲洗 30 秒以上，配合手搓效果最好；2. 小苏打水（1 升水加 1 勺）浸泡 10-15 分钟再冲净，对有机磷类农残降解效果好；3. 去皮可去除绝大部分残留；4. 叶菜切除根部后逐叶清洗。不建议长时间盐水浸泡，反而可能产生二次污染。') AS t
WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '消费者清洗蔬果去农残技巧');

INSERT INTO knowledge_doc (title, category, content)
SELECT * FROM (SELECT '施肥与农家肥安全使用', '种植标准',
'种植过程中农家肥（鸡粪、牛粪等）必须充分腐熟后使用，未腐熟农家肥携带大肠杆菌等病原菌并可能污染土壤。化肥应按测土配方施肥，采收前 15 天停止追施速效氮肥，避免硝酸盐积累超标。有机认证地块禁止使用化学合成氮肥。') AS t
WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '施肥与农家肥安全使用');

-- ===== 以下为 2026-10 扩充批次（覆盖 GB 标准/病虫害/储存/检测/流通/科普，共 100 条）=====

INSERT INTO knowledge_doc (title, category, content)
SELECT * FROM (SELECT 'GB2763 根茎类蔬菜农残限量', '法规标准',
'GB 2763 对根茎类蔬菜（萝卜、胡萝卜、土豆）规定：毒死蜱限量 0.1 mg/kg，敌百虫 0.1 mg/kg，甲氰菊酯 0.5 mg/kg。土豆还需关注氯苯胺灵（抑芽剂）限量 30 mg/kg。检测报告数值低于限量即合格。') AS t
WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = 'GB2763 根茎类蔬菜农残限量');

INSERT INTO knowledge_doc (title, category, content)
SELECT * FROM (SELECT 'GB2763 甘蓝类蔬菜农残限量', '法规标准',
'GB 2763 对甘蓝类（包菜、花椰菜、西兰花）规定：虫螨腈限量 2 mg/kg，茚虫威 1 mg/kg，阿维菌素 0.05 mg/kg，氟虫腈 0.02 mg/kg。甘蓝类生长期长、用药机会多，是抽检重点品类。') AS t
WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = 'GB2763 甘蓝类蔬菜农残限量');

INSERT INTO knowledge_doc (title, category, content)
SELECT * FROM (SELECT '国家禁用限用农药清单常识', '法规标准',
'国家明令禁止生产、销售和使用的农药包括：甲拌磷、克百威、涕灭威、氧乐果（部分作物）、水胺硫磷、甲基异柳磷、灭多威、杀扑磷等高毒品种。蔬菜、瓜果、茶叶、中草药材上不得使用、限制使用的农药另有名录。发现使用禁用农药属违法行为，可直接投诉举报。') AS t
WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '国家禁用限用农药清单常识');

INSERT INTO knowledge_doc (title, category, content)
SELECT * FROM (SELECT '农产品质量安全法消费者权益', '法规标准',
'《中华人民共和国农产品质量安全法》规定：农产品生产者应当建立生产记录，销售者应当履行进货查验义务；消费者购买到不符合质量安全标准的农产品，有权要求赔偿，并可向市场监管部门 12315 或农业部门投诉。平台溯源记录可作维权凭证。') AS t
WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '农产品质量安全法消费者权益');

INSERT INTO knowledge_doc (title, category, content)
SELECT * FROM (SELECT '绿色食品标准体系常识', '法规标准',
'绿色食品执行 NY/T 行业标准，分 AA 级（不使用化学合成农药化肥）和 A 级（限量使用低毒低残留农药）。绿色食品标志有效期 3 年，产品需通过产地环境、生产过程、产品质量、包装运输四环节审核。包装上有绿色食品标志编号可在中国绿色食品发展中心官网查询真伪。') AS t
WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '绿色食品标准体系常识');

INSERT INTO knowledge_doc (title, category, content)
SELECT * FROM (SELECT '有机产品认证常识', '法规标准',
'有机产品（Organic）认证依据 GB/T 19630 标准，要求生产过程不使用化学合成的农药、化肥、生长调节剂，不采用基因工程技术，转换期一般 2-3 年。正规有机产品包装应有有机码，可登录 food.cnca.cn 全国认证认可信息平台查询。无标志无编码的所谓有机菜不可信。') AS t
WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '有机产品认证常识');

INSERT INTO knowledge_doc (title, category, content)
SELECT * FROM (SELECT '食用农产品承诺达标合格证制度', '法规标准',
'食用农产品承诺达标合格证制度要求种植养殖生产者开具合格证，承诺不使用禁用农药兽药、不超过安全间隔期、符合质量安全标准。合格证内容包括生产者名称、产品名称、数量、开具日期。消费者扫码溯源时可核对批次检测报告与合格证信息是否一致。') AS t
WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '食用农产品承诺达标合格证制度');

INSERT INTO knowledge_doc (title, category, content)
SELECT * FROM (SELECT 'GB7718 预包装食品标签通则', '法规标准',
'GB 7718 规定预包装食品标签必须标注：食品名称、配料表、净含量、生产者名称地址、生产日期、保质期、贮存条件、食品生产许可证编号。果蔬类初加工产品若经清洗切分包装销售，同样需符合标签要求，无生产日期的包装净菜不要购买。') AS t
WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = 'GB7718 预包装食品标签通则');

INSERT INTO knowledge_doc (title, category, content)
SELECT * FROM (SELECT '重金属污染物限量标准 GB2762', '法规标准',
'GB 2762《食品中污染物限量》规定叶菜类蔬菜铅限量 0.3 mg/kg、镉 0.2 mg/kg，根茎类铅 0.25 mg/kg、镉 0.1 mg/kg。工业矿区周边种植的农产品重金属超标风险高，选择有产地环境检测的溯源基地产品更安全。') AS t
WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '重金属污染物限量标准 GB2762');

INSERT INTO knowledge_doc (title, category, content)
SELECT * FROM (SELECT '农药标签三证常识', '法规标准',
'正规农药标签必须有：农药登记证号（PD 开头）、生产许可证号（XK）、产品标准号三证信息，以及有效成分含量、登记作物、防治对象、使用剂量、安全间隔期。购买农药时可在 中国农药信息网 查询登记证真伪，无三证的产品属假农药。') AS t
WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '农药标签三证常识');

INSERT INTO knowledge_doc (title, category, content)
SELECT * FROM (SELECT '生鲜电商七天无理由退货例外', '法规标准',
'根据消费者权益保护法，生鲜易腐农产品一般不适用七天无理由退货，但商品存在质量问题（腐烂、霉变、货不对板）不受此限。收到生鲜问题商品应在签收时或 24 小时内拍照留证，联系商家协商，协商不成可向平台投诉或拨打 12315 维权。') AS t
WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '生鲜电商七天无理由退货例外');

INSERT INTO knowledge_doc (title, category, content)
SELECT * FROM (SELECT '食品中污染物与真菌毒素限量', '法规标准',
'GB 2761 规定食品中真菌毒素限量：花生玉米黄曲霉毒素 B1 限量 20 微克/公斤，小麦粉 5 微克/公斤。采购粮油类农产品时应查看检测报告中的真菌毒素项目，霉变粒超标的花生玉米绝对不可食用，黄曲霉毒素为强致癌物且高温烹煮无法分解。') AS t
WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '食品中污染物与真菌毒素限量');

INSERT INTO knowledge_doc (title, category, content)
SELECT * FROM (SELECT '无公害农产品产地认定', '法规标准',
'无公害农产品产地认定要求产地环境符合 NY/T 5010 标准：土壤重金属背景值正常、灌溉水质达标、远离工矿污染源 3 公里以上、大气环境质量良好。产地认定证书有效期 3 年，溯源系统中的基地信息可对接产地证书编号供消费者核验。') AS t
WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '无公害农产品产地认定');

INSERT INTO knowledge_doc (title, category, content)
SELECT * FROM (SELECT '植物检疫与调运许可', '法规标准',
'跨区域调运种子、种苗及部分农产品需植物检疫证书，防止检疫性有害生物（如红火蚁、柑橘黄龙病、番茄溃疡病）传播。经销商批量调运蔬菜瓜果应索取产地检疫证明，溯源流转记录中登记检疫信息可提升批次可信度。') AS t
WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '植物检疫与调运许可');

INSERT INTO knowledge_doc (title, category, content)
SELECT * FROM (SELECT '生物农药与化学农药区别', '法规标准',
'生物农药源于生物活体或其代谢产物，如苏云金杆菌（Bt）、枯草芽孢杆菌、苦参碱、印楝素，通常低毒易降解、安全间隔期短；化学合成农药见效快但需严格执行安全间隔期。绿色种植优先选用生物农药，溯源农事记录中登记用药类型可帮消费者判断种植规范程度。') AS t
WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '生物农药与化学农药区别');

INSERT INTO knowledge_doc (title, category, content)
SELECT * FROM (SELECT '农药残留快速检测技术', '法规标准',
'酶抑制率快速检测法（速测卡/速测仪）可在 15-30 分钟内筛查有机磷和氨基甲酸酯类农残，抑制率≥50% 判为阳性需复检。快检是市场准入初筛手段，法定仲裁以实验室色谱-质谱法为准。快检阳性但实验室复检合格的批次以实验室结论为准。') AS t
WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '农药残留快速检测技术');

INSERT INTO knowledge_doc (title, category, content)
SELECT * FROM (SELECT '蔬菜水果铅镉污染来源', '法规标准',
'农产品铅镉污染主要来源于：工业废气沉降污染表土、含镉磷肥过量施用、污水灌溉。铅主要富积在叶片（叶菜>根茎>瓜果），镉易在水稻和叶菜中积累。选购时关注产地信息，避开冶炼厂、电镀厂周边产地的叶菜类。') AS t
WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '蔬菜水果铅镉污染来源');

INSERT INTO knowledge_doc (title, category, content)
SELECT * FROM (SELECT '温室大棚种植环境管理', '种植标准',
'日光温室管理要点：白天温度控制在 25-30 度（茄果类）、夜间 15-18 度；湿度控制在 60-75% 可显著降低病害发生；及时通风换气补充二氧化碳。棚内高温高湿是灰霉病、霜霉病高发主因，农事记录中的放风时间与温湿度登记是评价管理水平的参考。') AS t
WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '温室大棚种植环境管理');

INSERT INTO knowledge_doc (title, category, content)
SELECT * FROM (SELECT '番茄灰霉病识别与防治', '种植标准',
'番茄灰霉病在低温高湿（15-23 度、湿度 90% 以上）易发，表现为叶片出现 V 形褐斑、果实蒂部生灰色霉层。防治：及时摘除病果病叶带出棚外销毁、控制浇水加强通风、发病初期喷施腐霉利或嘧霉胺，注意轮换用药防抗性，安全间隔期按标签执行。') AS t
WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '番茄灰霉病识别与防治');

INSERT INTO knowledge_doc (title, category, content)
SELECT * FROM (SELECT '黄瓜霜霉病识别与防治', '种植标准',
'黄瓜霜霉病典型症状是叶片正面出现多角形黄褐斑、背面生灰黑色霉层，高湿通风差时迅速蔓延。预防为主：选用抗病品种、地膜覆盖降低棚内湿度、发病前用代森锰锌保护性喷施，发病后用烯酰吗啉或霜脲氰治疗，每 7 天一次连续 2-3 次。') AS t
WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '黄瓜霜霉病识别与防治');

INSERT INTO knowledge_doc (title, category, content)
SELECT * FROM (SELECT '白粉病综合防治', '种植标准',
'白粉病在黄瓜、南瓜、草莓上高发，叶面出现白色粉状霉斑。防治要点：合理密植增强通风透光、避免偏施氮肥、发病初期喷施醚菌酯或氟菌唑，生物防治可用枯草芽孢杆菌。棚室可高温闷棚（36-40 度保持 2 小时）抑制病菌，注意闷棚前一日浇足水防烧苗。') AS t
WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '白粉病综合防治');

INSERT INTO knowledge_doc (title, category, content)
SELECT * FROM (SELECT '番茄早疫晚疫病区别与防治', '种植标准',
'早疫病叶面出现同心轮纹状褐斑（靶斑），高温高湿发病；晚疫病叶缘生水浸状绿褐斑、背面有白色霉层，低温高湿（15-22 度）流行速度快，可数日内毁棚。晚疫病须立即喷施氟菌霜霉威或烯酰吗啉并清除病株，早疫病用苯醚甲环唑或代森锰锌防治。') AS t
WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '番茄早疫晚疫病区别与防治');

INSERT INTO knowledge_doc (title, category, content)
SELECT * FROM (SELECT '蚜虫绿色防控技术', '种植标准',
'蚜虫聚集嫩叶背面吸汁造成卷叶并传播病毒病。绿色防控组合：棚内张挂黄色粘虫板（每亩 30 块，悬挂植株上部 20 厘米）、通风口设 40 目防虫网、释放丽蚜小蜂等天敌；化学防治用吡虫啉或噻虫嗪，重点喷施叶背，采收前按安全间隔期停药。') AS t
WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '蚜虫绿色防控技术');

INSERT INTO knowledge_doc (title, category, content)
SELECT * FROM (SELECT '白粉虱防治方法', '种植标准',
'温室白粉虱（小白蛾）群集叶背吸汁并分泌蜜露诱发煤污病。综合防治：黄板诱杀成虫、通风口加防虫网阻断迁入、冬季休棚期低温冻棚杀灭越冬虫源、药剂可选噻虫嗪或呋虫胺喷雾，配合联苯菊酯熏棚效果更好，注意不同作用机理轮换使用。') AS t
WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '白粉虱防治方法');

INSERT INTO knowledge_doc (title, category, content)
SELECT * FROM (SELECT '菜青虫与斜纹夜蛾防治', '种植标准',
'菜青虫（菜粉蝶幼虫）啃食甘蓝白菜叶片成孔洞，斜纹夜蛾幼虫群集暴食且昼伏夜出。首选生物农药苏云金杆菌（Bt）或甲维盐，傍晚施药效果好（夜蛾类夜间取食）；成虫期可用性诱剂或杀虫灯诱杀，卵期释放赤眼蜂。收获前 5-7 天停止用药。') AS t
WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '菜青虫与斜纹夜蛾防治');

INSERT INTO knowledge_doc (title, category, content)
SELECT * FROM (SELECT '潜叶蝇识别与防治', '种植标准',
'潜叶蝇幼虫在叶片内蛀食形成蜿蜒白色隧道（鬼画符），影响光合作用与商品性。防治：摘除受害叶片集中处理、张挂黄板诱杀成虫、始见隧道时喷施灭蝇胺或阿维菌素，7 天一次连续 2 次。育苗期是防治关键窗口，带虫苗定植后虫害加重。') AS t
WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '潜叶蝇识别与防治');

INSERT INTO knowledge_doc (title, category, content)
SELECT * FROM (SELECT '红蜘蛛（叶螨）防治要点', '种植标准',
'红蜘蛛干旱高温（25-30 度、湿度低于 60%）时爆发，叶面出现密集黄白色失绿小点。防治关键：及时浇水提高湿度、摘除重害叶、用阿维菌素或螺螨酯叶背喷雾（卵螨兼治），间隔 5-7 天连续 2 次。棚室湿度管理是最经济的预防手段。') AS t
WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '红蜘蛛（叶螨）防治要点');

INSERT INTO knowledge_doc (title, category, content)
SELECT * FROM (SELECT '根结线虫危害与防控', '种植标准',
'根结线虫侵染根系形成瘤状根结，植株矮化黄萎、结果少，重茬棚室危害重。防控：夏季高温闷棚（覆膜灌水 55 度以上 15 天）、轮作葱蒜类或种植万寿菊绿肥、定植前沟施淡紫拟青霉等生物菌剂，化学药剂可用噻唑膦颗粒剂严格按标签用量。') AS t
WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '根结线虫危害与防控');

INSERT INTO knowledge_doc (title, category, content)
SELECT * FROM (SELECT '土壤连作障碍与轮作制度', '种植标准',
'同科蔬菜连作 3 年以上会出现土传病害加重、产量品质下降的连作障碍（如番茄青枯病、白菜软腐病）。科学轮作原则：茄科（番茄辣椒）与葫芦科（黄瓜南瓜）或十字花科轮作，忌同科连作；水旱轮作效果最佳；配合增施腐熟有机肥和微生物菌剂改良土壤。') AS t
WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '土壤连作障碍与轮作制度');

INSERT INTO knowledge_doc (title, category, content)
SELECT * FROM (SELECT '种子处理与浸种催芽', '种植标准',
'播种前种子处理可防苗期病害：温汤浸种（55 度热水搅拌 15 分钟）杀灭种子表面病菌；药剂消毒可用 10% 磷酸三钠浸番茄种子 20 分钟防病毒病；催芽保持 25-28 度保湿至露白播种。溯源农事记录中的种子处理信息反映苗期健康管理水平。') AS t
WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '种子处理与浸种催芽');

INSERT INTO knowledge_doc (title, category, content)
SELECT * FROM (SELECT '滴灌与水肥一体化', '种植标准',
'滴灌比漫灌节水 40-60% 且降低棚内湿度减少病害，配合施肥罐实现水肥一体化：苗期以氮为主促茎叶，开花坐果期增磷钾，结果盛期少量多次。追肥浓度控制在 0.2-0.3%，避免烧根。农事记录中的灌溉日期与水量是判断种植管理规范性的重要依据。') AS t
WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '滴灌与水肥一体化');

INSERT INTO knowledge_doc (title, category, content)
SELECT * FROM (SELECT '微量元素缺乏症识别', '种植标准',
'常见缺素症状：缺钙番茄脐腐病（果蒂黑斑）、缺镁下部叶片脉间黄化、缺铁新叶整叶黄白、缺硼生长点萎缩和果实木栓化。防治：叶面喷施对应微量元素肥（0.2% 磷酸二氢钾补磷钾、糖醇钙补钙），底肥增施有机肥从根本上改善微量元素供应。') AS t
WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '微量元素缺乏症识别');

INSERT INTO knowledge_doc (title, category, content)
SELECT * FROM (SELECT '熊蜂授粉与激素坐果', '种植标准',
'番茄棚室可用熊蜂授粉替代激素蘸花，果形圆润、汁多籽实、无激素残留顾虑，蜂箱使用期间严禁喷施杀虫剂。激素坐果（对氯苯氧乙酸）是常规手段，但浓度过高会造成果实畸形或空心。消费者可通过种子发育状态判断：有籽饱满多为自然授粉或熊蜂授粉。') AS t
WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '熊蜂授粉与激素坐果');

INSERT INTO knowledge_doc (title, category, content)
SELECT * FROM (SELECT '采收成熟度与品质关系', '种植标准',
'番茄八成熟（转色期）采收耐储运、风味略淡，全红采收风味最佳但货架期短；黄瓜达到商品瓜长 25-30 厘米及时采收，过老则瓜条发苦变黄。带果柄采收减少伤口腐损。溯源信息中的采收日期与成熟度说明可帮消费者判断新鲜度。') AS t
WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '采收成熟度与品质关系');

INSERT INTO knowledge_doc (title, category, content)
SELECT * FROM (SELECT '黄板与防虫网物理防控', '种植标准',
'物理防控是绿色种植首选：黄色粘虫板诱杀蚜虫白粉虱斑潜蝇（悬挂高度植株冠层上方 10-20 厘米，每亩 30-40 块，粘满更换）；40-60 目防虫网阻隔大部分害虫迁入；杀虫灯对夜蛾类金龟子诱杀半径 80-120 米。溯源记录中有物理防控信息说明种植管理更规范。') AS t
WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '黄板与防虫网物理防控');

INSERT INTO knowledge_doc (title, category, content)
SELECT * FROM (SELECT '大棚高温闷棚消毒', '种植标准',
'夏季休茬期高温闷棚是棚室土壤消毒经济有效的方法：清园后深翻 30 厘米、大水漫灌、全棚覆盖地膜、密闭棚室 15-20 天，地表 5 厘米温度可达 50 度以上杀灭大部分土传病菌与线虫。结合撒施碎秸秆+发酵菌剂效果更好，闷棚后补充生物菌肥重建有益菌群。') AS t
WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '大棚高温闷棚消毒');

INSERT INTO knowledge_doc (title, category, content)
SELECT * FROM (SELECT '黄瓜采后储存方法', '储存保鲜',
'黄瓜适宜储存温度 10-13 度、湿度 90-95%，冷藏可存 5-7 天；低于 8 度易发生冷害（出水、塌陷、苦味加重）。注意黄瓜怕乙烯，勿与苹果香蕉番茄同放。表面有刺的新鲜黄瓜刺硬扎手，放软即不新鲜。') AS t
WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '黄瓜采后储存方法');

INSERT INTO knowledge_doc (title, category, content)
SELECT * FROM (SELECT '辣椒茄子储存方法', '储存保鲜',
'辣椒适宜 8-12 度储存，怕冻怕闷，冷藏用保鲜袋扎孔透气可存 7-10 天；茄子适宜 10-12 度，表皮蜡质层是天然保护，储存前不要清洗，碰伤处易发黑腐烂，用纸包裹冷藏可存 5-7 天。两者均不可低于 5 度，否则出现冷害水渍斑。') AS t
WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '辣椒茄子储存方法');

INSERT INTO knowledge_doc (title, category, content)
SELECT * FROM (SELECT '土豆储存与发芽防控', '储存保鲜',
'土豆适宜储存温度 4-10 度、避光、干燥、通风，见光会产生叶绿素与龙葵碱（发芽变绿部分含量高，有毒性）。苹果释放的乙烯可抑制土豆发芽，可同箱放几个苹果。已发芽变绿的土豆龙葵碱含量超标的应整颗丢弃，削芽不足以去除毒素。') AS t
WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '土豆储存与发芽防控');

INSERT INTO knowledge_doc (title, category, content)
SELECT * FROM (SELECT '洋葱大蒜生姜储存', '储存保鲜',
'洋葱大蒜耐储：干燥通风常温（15-25 度）可存 1-3 个月，忌潮湿（发芽发霉）。生姜适宜 12-15 度避光，冷藏易受冻，可用保鲜膜包好放阴凉处或埋入沙中。三者均不建议冷藏，家庭常温阴凉处存放即可，出芽的蒜和姜仍可食用。') AS t
WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '洋葱大蒜生姜储存');

INSERT INTO knowledge_doc (title, category, content)
SELECT * FROM (SELECT '胡萝卜白萝卜储存', '储存保鲜',
'胡萝卜白萝卜适宜 0-5 度高湿储存，切去缨叶（叶会抢走根部水分），带泥更耐存，冷藏可存 2-4 周。白萝卜开裂或糠心是存放过久失水所致。胡萝卜与苹果梨同放会产生苦味（乙烯影响），应分开存放。') AS t
WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '胡萝卜白萝卜储存');

INSERT INTO knowledge_doc (title, category, content)
SELECT * FROM (SELECT '大白菜与菠菜储存差异', '储存保鲜',
'大白菜耐储，0-5 度可存 2-4 周，外层叶有保护作用，吃时再剥；菠菜生菜等柔嫩叶菜呼吸强，0-4 度仅存 3-5 天，需厨房纸包裹吸潮再装袋。白菜帮有黑斑（储存中生理性病变）面积大时建议弃食，小黑点不影响安全但口感差。') AS t
WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '大白菜与菠菜储存差异');

INSERT INTO knowledge_doc (title, category, content)
SELECT * FROM (SELECT '苹果储存与乙烯释放', '储存保鲜',
'苹果是乙烯释放大户，4 度冷藏可存 1-2 个月，常温可存 2 周。切记与叶菜、黄瓜、胡萝卜隔离存放，乙烯会导致叶菜黄化、黄瓜发苦、萝卜发苦。家庭可把需后熟的水果（猕猴桃、芒果、香蕉）与苹果同袋催熟。') AS t
WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '苹果储存与乙烯释放');

INSERT INTO knowledge_doc (title, category, content)
SELECT * FROM (SELECT '草莓葡萄浆果类保鲜', '储存保鲜',
'草莓极不耐储：0-2 度冷藏仅存 2-3 天，且入库前不要清洗（水洗后霉变加快），吃前再用流水冲洗。葡萄耐储性较好，0-2 度可存 5-7 天，表面白霜是天然果粉是新鲜的标志，掉粒、果柄干枯说明不新鲜。浆果类霉变会产生展青霉素，一颗发霉建议整盒弃食。') AS t
WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '草莓葡萄浆果类保鲜');

INSERT INTO knowledge_doc (title, category, content)
SELECT * FROM (SELECT '柑橘类水果储存', '储存保鲜',
'柑橘橙柚适宜 5-10 度储存，湿度 85-90%，可存 2-4 周；过冷（低于 3 度）会发生冷害（果皮凹陷、异味）。表皮机械伤和霉斑果要及时挑出，霉菌会扩散传染整箱。柚子类厚皮果常温阴凉存放即可，越放越甜（淀粉糖化）。') AS t
WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '柑橘类水果储存');

INSERT INTO knowledge_doc (title, category, content)
SELECT * FROM (SELECT '冷链预冷技术常识', '储存保鲜',
'采后预冷是冷链第一环：采收后 2 小时内将果蔬温度降至储运要求，可去除田间热、延长货架期 1-2 倍。常见方式：冷风预冷（通用）、真空预冷（叶菜 20-30 分钟降至 4 度）、冰水预冷（根茎类）。无预冷的常温运输，叶菜一天失水 5-10%。') AS t
WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '冷链预冷技术常识');

INSERT INTO knowledge_doc (title, category, content)
SELECT * FROM (SELECT '乙烯催熟与保鲜原理', '储存保鲜',
'乙烯是果蔬自身释放的催熟激素：猕猴桃、香蕉、芒果、番茄采后持续释放乙烯后熟；乙烯敏感型果蔬（叶菜、黄瓜、胡萝卜）受乙烯影响加速衰老变黄。储存原则：呼吸高峰型水果（苹果香蕉）与呼吸非高峰型蔬菜分区存放，冷库可用乙烯脱除机或高锰酸钾载体吸附。') AS t
WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '乙烯催熟与保鲜原理');

INSERT INTO knowledge_doc (title, category, content)
SELECT * FROM (SELECT '果蔬冻害识别与预防', '储存保鲜',
'热带亚热带果蔬（香蕉、芒果、番茄、黄瓜、青椒）在 0-4 度普通冷藏易发生冷害：表皮水渍状凹陷、褐变、不能正常后熟变味。香蕉适宜 12-14 度，芒果 10-13 度。冰箱冷藏不是万能，热带水果常温存放更快更安全，已发生冷害的尽快食用。') AS t
WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '果蔬冻害识别与预防');

INSERT INTO knowledge_doc (title, category, content)
SELECT * FROM (SELECT '净菜加工卫生要求', '储存保鲜',
'净菜（切分即食蔬菜）加工要求：原料经清洗消毒（次氯酸钙 100-200ppm 浸泡再清水漂洗）、切割刀具案板生熟分开、全程 10 度以下低温操作、包装后 0-4 度冷链销售，保质期一般 3-7 天。切分蔬菜营养流失快，购买后尽快食用，出现异味发黏立即丢弃。') AS t
WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '净菜加工卫生要求');

INSERT INTO knowledge_doc (title, category, content)
SELECT * FROM (SELECT '家庭冰箱分区存放原则', '储存保鲜',
'冰箱科学分区：冷藏室上层（温度稳定）放剩菜熟食；中层放乳制品豆制品；下层（最冷）放需即食的生鲜肉类；果蔬抽屉高湿度区放蔬菜水果。生熟分离、包装密封防串味与交叉污染。冰箱不是保险箱，冷藏仅延缓变质，2-3 天内食用完最佳。') AS t
WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '家庭冰箱分区存放原则');

INSERT INTO knowledge_doc (title, category, content)
SELECT * FROM (SELECT '果蔬失水萎蔫与保湿', '储存保鲜',
'果蔬采后失水是品质下降主因：失水超 5% 外观明显萎蔫。保湿要点：叶菜用带孔保鲜袋（防缺氧）、根茎类带泥或用微湿沙埋、瓜果类可涂果蜡保鲜。冷库湿度一般维持 85-95%。家庭蔬菜蔫了可泡清水 30 分钟部分恢复脆度，但营养已流失。') AS t
WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '果蔬失水萎蔫与保湿');

INSERT INTO knowledge_doc (title, category, content)
SELECT * FROM (SELECT '气调冷藏（CA）保鲜技术', '储存保鲜',
'气调冷藏通过降低氧气（2-5%）提高二氧化碳（3-5%）浓度，抑制果蔬呼吸作用，比普通冷藏延长保鲜期 0.5-1 倍。苹果梨大蒜等耐低氧品种效果好，消费者日常可用保鲜盒或保鲜膜营造简易小气调环境。溯源流通记录中标注气调库储存可提升产品档次与可信度。') AS t
WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '气调冷藏（CA）保鲜技术');

INSERT INTO knowledge_doc (title, category, content)
SELECT * FROM (SELECT '鲜食玉米与豆类储存', '储存保鲜',
'甜玉米采后糖分转化极快：常温 1 天损失一半甜度，须立即预冷 0-2 度带皮储存，3 天内食用或冷冻（焯水后冷冻可存 6 个月）。毛豆、豌豆等豆荚类同样呼吸旺盛，0-4 度仅存 3-5 天，久放豆粒变黄发硬，适合焯水冷冻长期保存。') AS t
WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '鲜食玉米与豆类储存');

INSERT INTO knowledge_doc (title, category, content)
SELECT * FROM (SELECT '食用菌储存注意事项', '储存保鲜',
'鲜香菇平菇等食用菌 0-4 度可存 3-5 天，怕捂怕压：用纸袋或扎孔保鲜袋包装吸潮，塑料袋密封易发黏变质。菌类清洗时机：食用前再洗，提前水洗吸水后加快腐败。出现发黏、酸味、菌盖发黑即变质，蘑菇中毒风险高，不认识的野生菌绝不可食用。') AS t
WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '食用菌储存注意事项');

INSERT INTO knowledge_doc (title, category, content)
SELECT * FROM (SELECT 'CMA 与 CNAS 检测报告资质', '检测知识',
'查看检测报告先看资质章：CMA（中国计量认证）是强制性的市场准入资质，有 CMA 章的报告才有法律效力；CNAS 是实验室能力国家认可，表示结果国际互认。两者都有最权威。无资质章、无检测人签字、无报告编号的报告不可信，可在省级市场监管网站查报告真伪。') AS t
WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = 'CMA 与 CNAS 检测报告资质');

INSERT INTO knowledge_doc (title, category, content)
SELECT * FROM (SELECT '农残检测抽样规范', '检测知识',
'规范抽样决定检测结果真实性：按 GB 23200 与 NY/T 789 执行，随机多点取样（田块对角线或 S 形布点 5-10 处）、取样量 1-2 公斤、样品 24 小时内低温送检、封样双方签字。只取长势好的一株或一块地采样属于无效抽样，溯源系统对接第三方抽检报告更有说服力。') AS t
WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '农残检测抽样规范');

INSERT INTO knowledge_doc (title, category, content)
SELECT * FROM (SELECT '农残检测主要项目分类', '检测知识',
'蔬菜常检农残项目分四类：有机磷类（毒死蜱、敌敌畏）、氨基甲酸酯类（克百威、灭多威）、拟除虫菊酯类（氯氰菊酯、溴氰菊酯）、新烟碱类（吡虫啉、噻虫嗪）。正规报告会列出具体检测项目与限量值，只写农残合格四个字而无数据的报告属于不规范报告。') AS t
WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '农残检测主要项目分类');

INSERT INTO knowledge_doc (title, category, content)
SELECT * FROM (SELECT '重金属检测项目与方法', '检测知识',
'农产品重金属检测常测铅、镉、汞、砷、铬五项，按 GB 5009 系列标准用原子吸收光谱或 ICP-MS 定量。判定依据 GB 2762 污染物限量。叶菜类最易超标的是铅和镉，大米重点看镉。溯源批次如附重金属检测数据，是高端品牌的重要背书。') AS t
WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '重金属检测项目与方法');

INSERT INTO knowledge_doc (title, category, content)
SELECT * FROM (SELECT '检测不合格批次的处置流程', '检测知识',
'检出农残超标后：样品复检确认→通知生产者暂停采收销售→追溯同批次流向并下架封存→查明原因（违规用药、未过安全间隔期、污染）→整改后重新检测合格方可恢复上市。按农产品质量安全法，销售不合格农产品将面临没收违法所得、罚款直至刑事责任。') AS t
WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '检测不合格批次的处置流程');

INSERT INTO knowledge_doc (title, category, content)
SELECT * FROM (SELECT '检测报告有效期与批次对应', '检测知识',
'检测报告只对来样批次负责，不存在一年有效的通用检测报告。同一地块不同批次用药不同，需分别检测；同一批次分次采收的，抽检覆盖即可。消费者扫码时应核对：报告上的样品编号、采收日期与溯源批次信息是否对应，张冠李戴的报告无意义。') AS t
WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '检测报告有效期与批次对应');

INSERT INTO knowledge_doc (title, category, content)
SELECT * FROM (SELECT '硝酸盐含量与蔬菜安全', '检测知识',
'叶菜类易积累硝酸盐（施氮肥过多或光照不足时），GB 2762 对部分蔬菜有硝酸盐限量（叶菜类最高 3000 mg/kg 依产地分级）。家庭降低硝酸盐摄入：外层老叶含量最高剥去 2-3 片、沸水焯烫可去除 30-50%、避免隔夜久存。婴儿辅食建议选硝酸盐含量低的瓜果类。') AS t
WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '硝酸盐含量与蔬菜安全');

INSERT INTO knowledge_doc (title, category, content)
SELECT * FROM (SELECT '第三方检测机构如何选择', '检测知识',
'选择农残检测机构要点：具有 CMA 资质且能力范围覆盖目标检测项目（可在资质认定证书附表核对）、检测周期 3-7 个工作日、出具带限量判定结论的报告。费用参考：常规农残多残留筛查（50 项左右）约 800-2000 元/样。溯源平台对接的检测机构信息应公开供查询。') AS t
WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '第三方检测机构如何选择');

INSERT INTO knowledge_doc (title, category, content)
SELECT * FROM (SELECT '微生物污染与大肠杆菌指标', '检测知识',
'即食蔬菜的微生物指标比农残更直接关系急性食源性疾病：大肠菌群、沙门氏菌、李斯特菌是重点检测项。污染途径主要是未腐熟粪肥、污染灌溉水、采收人员手部卫生。生食沙拉菜风险高于熟食蔬菜，免疫力低下人群建议熟制后食用。') AS t
WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '微生物污染与大肠杆菌指标');

INSERT INTO knowledge_doc (title, category, content)
SELECT * FROM (SELECT '经销商入库验收规范', '流通标准',
'经销商收货验收四查：一查感官（腐烂、机械伤、异味）、二查凭证（产地证明、合格证、检测报告）、三查批次（与订单批次号一致，拒收无标识货源）、四查温度（冷链货检测货箱中心温度，叶菜超 8 度可拒收）。验收信息录入溯源系统是承上启下的关键节点。') AS t
WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '经销商入库验收规范');

INSERT INTO knowledge_doc (title, category, content)
SELECT * FROM (SELECT '先进先出（FIFO）库存管理', '流通标准',
'生鲜库存执行先进先出原则：按批次入库时间先后出库，早入库先销售，避免压库变质。库位管理按批次分区、标识卡注明入库日期与保质期。每日盘点临期品（剩余货架期低于三分之一）优先促销处理。溯源系统中的出入库记录可自动校验 FIFO 执行情况。') AS t
WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '先进先出（FIFO）库存管理');

INSERT INTO knowledge_doc (title, category, content)
SELECT * FROM (SELECT '冷藏车运输温控要求', '流通标准',
'冷藏运输按货物类型设定温度：叶菜类 0-4 度、茄果瓜类 8-12 度、冷冻品 -18 度以下。装车前预冷车厢、堆码留通风道、门帘密封；全程温度记录仪监控，交接时打印温度曲线。断链（常温暴露超 2 小时）的冷链货质量风险大，收货方有权拒收。') AS t
WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '冷藏车运输温控要求');

INSERT INTO knowledge_doc (title, category, content)
SELECT * FROM (SELECT '生鲜包装标识规范', '流通标准',
'生鲜农产品销售包装应标注：品名、产地、生产日期或采收日期、重量、贮存条件、生产者或经销商信息。预包装净菜还需保质期与 SC 许可信息。散装销售应在陈列区公示产地与日期牌。溯源二维码印于包装上，消费者扫码即可核验包装信息与平台数据一致性。') AS t
WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '生鲜包装标识规范');

INSERT INTO knowledge_doc (title, category, content)
SELECT * FROM (SELECT '批发市场进货查验义务', '流通标准',
'《食品安全法》要求批发市场经营者履行进货查验：索取并保存供货方资质、产品合格证明（合格证、检测报告），建立进货台账保存不少于 6 个月。未履行查验义务导致问题产品流入的，经营者承担连带责任。电子溯源台账可替代纸质台账，是监管鼓励方向。') AS t
WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '批发市场进货查验义务');

INSERT INTO knowledge_doc (title, category, content)
SELECT * FROM (SELECT '临期生鲜的处理原则', '流通标准',
'临期生鲜（剩余货架期 1/3 以内）处理顺序：优先折价促销→员工内购→转加工（切配、腌制、熟制）→报废登记。不可篡改生产日期翻新销售，不可混入正常批次。报废需拍照登记原因，溯源系统中的报废记录帮助分析损耗环节，优化订货量。') AS t
WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '临期生鲜的处理原则');

INSERT INTO knowledge_doc (title, category, content)
SELECT * FROM (SELECT '追溯码编码规则常识', '流通标准',
'常见农产品追溯码形式：GB/T 38158 推荐一物一码或一批一码，编码可包含产地行政区划、产品代码、批次序列号。二维码内容一般指向溯源查询页 URL。消费者扫码核对要点：扫码显示的批次信息与包装标识的生产日期、产地是否一致，防止套牌冒用他人溯源码。') AS t
WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '追溯码编码规则常识');

INSERT INTO knowledge_doc (title, category, content)
SELECT * FROM (SELECT '冷链断链的风险与识别', '流通标准',
'冷链断链指运输储存中温度超出规定范围：反复冻融导致细胞破裂出水、微生物加速繁殖（每升温 10 度腐败速度翻倍）。消费者识别：包装内大量冰晶血水、叶菜冻伤透明化、肉类表面发黏。扫码溯源中的流转温度记录完整无断点，是高端冷链品牌的信任来源。') AS t
WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '冷链断链的风险与识别');

INSERT INTO knowledge_doc (title, category, content)
SELECT * FROM (SELECT '农产品损耗控制方法', '流通标准',
'生鲜流通损耗控制要点：订货精准（按历史销量 7 日滚动预测）、到货快速降温（先预冷再上架）、陈列控制（叶菜喷水保湿、避免灯光直射）、日清管理（晚市临期折扣）。行业平均损耗率：叶菜 15-25%、果类 8-15%。溯源数据辅助分析各环节损耗，指导供应链优化。') AS t
WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '农产品损耗控制方法');

INSERT INTO knowledge_doc (title, category, content)
SELECT * FROM (SELECT '如何挑选新鲜西红柿', '消费科普',
'新鲜西红柿挑选：果蒂翠绿、果脐收口紧密、表皮有光泽捏起来紧实有弹性。通体红得不自然且摸起来过软的可能催熟过头；果蒂周围发青的是未熟透（口感酸涩）。自然成熟的西红柿剖面籽粒饱满有汁，催熟果籽少汁水清。带蒂存放更耐放。') AS t
WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '如何挑选新鲜西红柿');

INSERT INTO knowledge_doc (title, category, content)
SELECT * FROM (SELECT '如何挑选新鲜黄瓜', '消费科普',
'新鲜黄瓜：瓜条直挺、表皮刺瘤完整扎手（顶花带刺）、瓜脐部颜色浅绿不发黄。瓜身发软打弯、刺脱落变黑说明采摘超 3 天。粗细均匀的口感好，过粗的可能空心籽老。购买后尽快冷藏，室温 2 天即失水变软。') AS t
WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '如何挑选新鲜黄瓜');

INSERT INTO knowledge_doc (title, category, content)
SELECT * FROM (SELECT '如何挑选叶菜类', '消费科普',
'叶菜挑选看三点：叶片挺括有光泽不塌蔫、叶柄断面湿润不发黑、无黄叶虫眼水渍斑。散装叶菜比包装净菜更新鲜（可看到完整状态），但注意喷水增重的（盆底积水多的摊位）。早市叶菜多为当日晨采，比下午摊更新鲜。扫码溯源可查到具体采收日期。') AS t
WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '如何挑选叶菜类');

INSERT INTO knowledge_doc (title, category, content)
SELECT * FROM (SELECT '时令蔬菜月历常识', '消费科普',
'应季蔬菜风味与营养最佳：春季（3-5 月）菠菜韭菜莴笋豌豆；夏季（6-8 月）番茄黄瓜茄子豆角冬瓜；秋季（9-11 月）南瓜萝卜白菜莲藕；冬季（12-2 月）大白菜萝卜土豆红薯。反季节菜多为大棚种植，品质不差但价格高。选时令菜既省钱又环保。') AS t
WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '时令蔬菜月历常识');

INSERT INTO knowledge_doc (title, category, content)
SELECT * FROM (SELECT '有机菜与普通菜营养价值', '消费科普',
'研究综述表明有机与常规种植蔬菜的宏量营养差异不大，有机菜的部分抗氧化物（多酚类）略高、农残检出率更低，但价格高 2-5 倍。性价比建议：常规菜按清洗规范处理已足够安全；预算有限时优先为儿童常吃的品类（菠菜草莓）选择有机或绿色认证产品。') AS t
WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '有机菜与普通菜营养价值');

INSERT INTO knowledge_doc (title, category, content)
SELECT * FROM (SELECT '隔夜菜的安全处理', '消费科普',
'隔夜菜安全要点：吃不完的菜 2 小时内（夏季 1 小时内）密封冷藏、下次食用彻底加热沸腾 3 分钟以上。亚硝酸盐风险主要在隔夜叶菜，建议叶菜当餐吃完，瓜果肉类冷藏 1-2 天内食用。反复加热的菜营养流失且易滋生细菌，分装小份加热一次吃完为宜。') AS t
WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '隔夜菜的安全处理');

INSERT INTO knowledge_doc (title, category, content)
SELECT * FROM (SELECT '食物中毒的早期识别与处置', '消费科普',
'食用农产品后 2-24 小时出现恶心呕吐、腹痛腹泻、头晕乏力，同餐多人发病，应怀疑食物中毒：立即停止食用并留样、轻者口服补液盐防脱水、出现高热血便意识模糊立即就医并拨打 12315 报告。保留购物凭证与剩余食物，是溯源追责与检测取样的关键证据。') AS t
WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '食物中毒的早期识别与处置');

INSERT INTO knowledge_doc (title, category, content)
SELECT * FROM (SELECT '农产品投诉维权流程', '消费科普',
'买到问题农产品维权五步：1. 拍照留证（商品+包装+标签+小票）2. 联系商家协商退赔 3. 平台介入申请 4. 拨打 12315 或全国 12315 平台小程序投诉 5. 涉及安全事故向市场监管部门举报。使用本平台扫码溯源的批次，可直接在溯源页提交投诉，工单直达管理员后台处理。') AS t
WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '农产品投诉维权流程');

INSERT INTO knowledge_doc (title, category, content)
SELECT * FROM (SELECT '五小农产品选购（葱姜蒜椒）', '消费科普',
'小料挑选：小葱叶尖不发黄根须湿润；姜选饱满无皱皮、断面浅黄（硫磺熏过的异常亮黄刺鼻）；蒜选硬实不发芽、蒜瓣完整；小米辣选果面光亮蒂绿。干料如干辣椒颜色过于鲜艳均匀的可能硫熏，闻有酸味刺鼻味的不要买。') AS t
WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '五小农产品选购（葱姜蒜椒）');

INSERT INTO knowledge_doc (title, category, content)
SELECT * FROM (SELECT '果蔬营养烹饪保留技巧', '消费科普',
'营养保留原则：先洗后切（切后水洗流失水溶性维生素）、急火快炒缩短加热、现切现炒现吃。番茄红素经加热更易吸收（熟吃好）；青椒西兰花维 C 怕热（快炒或白灼）；胡萝卜素脂溶性（用油炒吸收好）。凉拌菜用焯水 30 秒既去农残又保脆嫩。') AS t
WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '果蔬营养烹饪保留技巧');

INSERT INTO knowledge_doc (title, category, content)
SELECT * FROM (SELECT '农产品价格波动因素', '消费科普',
'菜价波动三因素：季节性（淡季运输成本高）、天气（暴雨寒潮短时推高叶菜价）、周期性（去年价高今年扩种导致过剩，即蛛网效应）。早市与晚市价差可达 30%，晚市适合买当日食用叶菜，早市适合买耐储菜。关注溯源产地信息，本地菜运输成本低更新鲜。') AS t
WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '农产品价格波动因素');

INSERT INTO knowledge_doc (title, category, content)
SELECT * FROM (SELECT '扫码溯源的意义与使用方法', '消费科普',
'农产品包装上的溯源二维码记录了从种植到销售的全链路信息：批次产地、农事记录（播种施肥打药浇水）、第三方检测报告、流转节点（农户→经销商）。使用方法：微信或浏览器扫码进入溯源页→查看批次档案→点击各环节时间线核实→有疑问直接在页面提交投诉。每扫一次码系统都会记录，扫码越多数据越可信。') AS t
WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '扫码溯源的意义与使用方法');

INSERT INTO knowledge_doc (title, category, content)
SELECT * FROM (SELECT '豇豆农残专项治理常识', '法规标准',
'豇豆（长豆角）因花期与采收期重叠、采摘频繁，是农残超标重灾区，被农业农村部列为专项治理品种。高风险项目为灭蝇胺、噻虫嗪、克百威。合规做法是登记用药、执行采摘安全间隔期。消费者购买豇豆建议查看检测报告，或焯水 2 分钟后再烹调更安心。') AS t
WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '豇豆农残专项治理常识');

INSERT INTO knowledge_doc (title, category, content)
SELECT * FROM (SELECT '韭菜腐霉利超标问题', '法规标准',
'韭菜曾长期存在腐霉利（防治灰霉病）残留超标问题，国家已将韭菜纳入重点治理品类并推行禁限用清单与安全间隔期管理。家庭处理：切除根部 1.5 厘米（农残富集区）、流水冲洗后浸泡。合规基地的韭菜有批批检测报告，扫码可见用药记录。') AS t
WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '韭菜腐霉利超标问题');

INSERT INTO knowledge_doc (title, category, content)
SELECT * FROM (SELECT '农产品地理标志保护', '法规标准',
'地理标志农产品（如五常大米、赣南脐橙）指产自特定地域、品质和声誉取决于产地自然与人文因素的农产品，受《农产品地理标志管理办法》保护。假冒产地属侵权行为。消费者可查包装上的地理标志专用标志及追溯信息，扫码溯源可核对批次产地是否在标志保护范围内。') AS t
WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '农产品地理标志保护');

INSERT INTO knowledge_doc (title, category, content)
SELECT * FROM (SELECT '读懂检测报告的步骤', '检测知识',
'三步读懂农残检测报告：第一步核对样品信息（样品名称、编号、抽样日期与购买批次是否对应）；第二步看检测结论（合格/不合格、是否附具体数值）；第三步对照限量（GB 2763 中对应作物的项目限值，实测值越低越好）。注意检出≠超标：低于限量的检出值属合格范围。') AS t
WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '读懂检测报告的步骤');

INSERT INTO knowledge_doc (title, category, content)
SELECT * FROM (SELECT '产地仓与销地仓模式', '流通标准',
'现代生鲜供应链常用产地仓（产地就近集货、分拣、预冷、包装）+ 销地仓（销区城市配送分拨）两级模式，减少中间装卸环节。产地仓预冷分级可提升售价 10-30%，销地仓支持次日达配送。溯源系统对接两级仓数据，消费者可查看批次完整流转轨迹。') AS t
WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '产地仓与销地仓模式');

INSERT INTO knowledge_doc (title, category, content)
SELECT * FROM (SELECT '西瓜挑选与膨大剂常识', '消费科普',
'挑西瓜三看一听：看纹路清晰间隔宽、看瓜脐内凹、看瓜蒂弯曲新鲜；拍打声音清脆带闷响为熟瓜。关于膨大剂（氯吡脲）：国家批准登记使用，降解快、瓜内残留极低，正常使用不影响安全；草莓畸形与西瓜空心更多与授粉和水分管理有关，不必谈激素色变。') AS t
WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '西瓜挑选与膨大剂常识');

INSERT INTO knowledge_doc (title, category, content)
SELECT * FROM (SELECT '草莓的正确清洗方法', '消费科普',
'草莓清洗正确步骤：先不去蒂用流水冲洗 30 秒（去蒂后浸泡会让污染物渗入果肉）、可用小苏打水浸泡 5-10 分钟、最后去蒂食用。清洗后的草莓不能再存放，随洗随吃。市售草莓霜霉灰霉病多发，表皮破损发软的果要挑出，整盒发霉建议弃食。') AS t
WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '草莓的正确清洗方法');

INSERT INTO knowledge_doc (title, category, content)
SELECT * FROM (SELECT '吡虫啉在番茄上的残留限量与安全间隔期', '法规标准',
'GB 2763-2021 规定吡虫啉在番茄（西红柿）中的最大残留限量（MRL）为 0.5 mg/kg。吡虫啉常用于防治蚜虫，标签推荐安全间隔期通常为 7 天；实际采收应不早于最后一次施药后 7 天，并以产品标签标注为准。检测报告实测值低于 0.5 mg/kg 即判定该项合格。') AS t
WHERE NOT EXISTS (SELECT 1 FROM knowledge_doc WHERE title = '吡虫啉在番茄上的残留限量与安全间隔期');
