# -*- coding: utf-8 -*-
"""生成《安溯·知源 VeriTrace 技术文档》：同时输出 DOCX（可编辑）与 PDF（提交用）。
内容与项目实际代码一致：Spring Boot 4.0.8 + Vue3 + 通义千问（Agent/RAG/多模态）。
"""
import os, sys, shutil

OUT_DIR = os.path.dirname(os.path.abspath(__file__))
DOCX_PATH = os.path.join(OUT_DIR, "安溯知源-技术文档.docx")
PDF_PATH = os.path.join(OUT_DIR, "安溯知源-技术文档.pdf")

TITLE = "安溯·知源 VeriTrace"
SUBTITLE = "基于大模型智能体的农产品可信溯源与信任者消费助手"
TEAM_LINE = "第九届传智杯 · AI WEB 网页开发挑战赛 · 技术文档"
DATE_LINE = "2026 年 11 月"

# ============================================================
# 内容模型：("h1"/"h2"/"h3"/"p"/"bullet"/"table"/"code"/"note"/"pb", payload)
# table payload: (caption, header[], rows[[]], widths_ratio[])
# ============================================================

def build_blocks():
    B = []
    A = B.append

    # ---------------- 第一章 ----------------
    A(("h1", "一、项目概述"))
    A(("h2", "1.1 项目背景"))
    A(("p", "农产品质量安全的核心矛盾，已经从「信息缺失」转为「信息不可信」。近年来各地建成的溯源平台不少，"
            "但普遍停留在「把数据录进去、把二维码印出来」的阶段：消费者扫码看到一串农事记录和一张检测报告照片，"
            "既没有能力判断这些数字意味着什么，也没有理由相信它们没有被事后补录。于是出现一个尴尬的循环——"
            "平台越强调「全链路」，消费者越觉得「这是商家自己填的」。"))
    A(("p", "我们在项目早期也走过同样的路：以「面向所有消费者的溯源查询平台」为定位，做了公开的溯源详情页、"
            "匿名投诉页、管理员数据大屏。实际推广中暴露了两个硬问题：其一，公开页面是匿名的，扫码即走，"
            "系统只留下 ip 与 user_agent，既认不出人，也留不下关系，复购与售后无处承接；"
            "其二，平台对「所有人都能查」负责，却不对「某个人敢不敢买」负责，"
            "在商业上无法形成可收费的闭环。"))
    A(("p", "因此项目在本阶段完成了一次主动收窄：从「大众化溯源平台」转为「信任者消费助手」。"
            "我们不做面向所有人的生鲜电商，我们做让每个农户都能经营自己信任者的消费助手。"))

    A(("h2", "1.2 产品定位"))
    A(("p", "「安溯·知源 VeriTrace」是一个面向「信任者」的农产品消费决策助手。它把一次性的扫码查询，"
            "改造为「建档—关注—提醒—复购—售后」的长期关系：消费者用手机号即可成为某位农户的信任者，"
            "关注在生长的批次、设置常购提醒、在疑问出现时获得有依据的答案、在出问题时看到承诺的响应时限。"))
    A(("table", ("表 1-1  平台四类用户与核心诉求", ["角色", "核心诉求", "平台给出的答案"], [
        ["农户", "把「我种得干净」证明出来，并沉淀自己的老客户", "批次档案、承诺清单、检测报告、邀请码与信任者台账"],
        ["经销商", "流转有据可查，责任边界清晰", "入库/出库台账、流转记录、扫码来源统计"],
        ["信任者（消费者）", "这块菜到底能不能放心买、怎么吃、下次什么时候买", "可信指数与证据链、AI 消费助手、常购提醒、售后 SLA"],
        ["平台管理员", "发现异常主体、督办超时售后", "信任运营大屏、投诉超时自动升级、批次与用户治理"],
    ], [0.18, 0.36, 0.46])))
    A(("p", "定位收窄并不等于市场变小。单个农户可以经营 200 名信任者，一个社区团长可以带来 300 户家庭，"
            "一家单位食堂一次覆盖上千人；平台以「可复制的信任单元」扩张，而不是以「全国消费者」的口号扩张。"))

    A(("h2", "1.3 核心创新点"))
    A(("bullet", [
        "确定性可信层与生成式决策层分离：可信指数由规则引擎计算、每条评分附带可展开的证据；大模型只负责「翻译」与「组织语言」，不负责打分。这样既避免了「AI 给农产品打分」的不可复核问题，又保留了大模型的表达优势。",
        "从「查一次」到「被记住」：消费者账户、关注关系、常购订阅、行为事件四类数据打通，助手回答时可以读取「我的常购清单」「我关注的农场」，做出个性化提醒，而不是通用话术。",
        "答案可溯源：RAG 命中的知识条目以「答案依据」形式展示标题、来源与版本，工具调用过程以 step 事件实时呈现，让用户看见 AI 是怎么得出结论的。",
        "承诺—履约—SLA 闭环：农户承诺写入 trust_promise，履约写入 fulfillment_record，售后投诉带 24 小时首次响应承诺并由定时任务标记超时，把「信任」变成可度量的运营指标。",
    ]))

    A(("h2", "1.4 大赛技术要求对应"))
    A(("p", "本项目的技术实现与「AI WEB 网页开发挑战赛」的五项硬指标逐条对应，具体落点如下表。"))
    A(("table", ("表 1-2  赛道硬指标与实现位置", ["赛道指标", "本项目实现", "核心代码/数据"], [
        ["现代前端框架", "Vue 3 + Vite + TypeScript + Element Plus + ECharts，SPA 由 Spring Boot 托管于 /app", "frontend/src（44 个 .vue/.ts 文件，5444 行）"],
        ["大模型集成", "通义千问 qwen-plus，DashScope OpenAI 兼容接口，同步与 SSE 流式双通道", "AgentService（837 行）、AiController"],
        ["AI Agent", "8 个工具、最多 5 轮工具调用、会话记忆 20 条、工具执行过程流式可见", "AgentService#chat / executeTool"],
        ["RAG", "102 条农事与消费知识，text-embedding-v3 向量化 + 余弦相似度 top-3，阈值 0.32", "KnowledgeService（193 行）、sql/ai_knowledge.sql"],
        ["多模态", "qwen3-vl-plus 识别消费者拍摄的实物照片，结果注入提示词后由 Agent 统一作答", "AiChat.vue 拍照上传 + AgentService 图片分支"],
    ], [0.20, 0.46, 0.34])))

    # ---------------- 第二章 ----------------
    A(("pb",))
    A(("h1", "二、需求分析"))
    A(("h2", "2.1 业务场景分析"))
    A(("p", "我们把真实使用过程拆成五个连续场景，每个场景都对应明确的系统行为与数据留存，"
            "而不是零散的页面功能。"))
    A(("table", ("表 2-1  典型场景与系统响应", ["序号", "场景", "系统响应与数据留存"], [
        ["S1", "消费者在包装上扫码", "跳转溯源详情页，展示可信指数、五维证据、农事与检测记录；同时写入 scan_log（含 consumer_id 与 scene），用于统计真实触达"],
        ["S2", "详情页转化为关系", "页面底部引导「成为信任者」与「加入常购」，写入 consumer_account / consumer_follow / consumer_subscription"],
        ["S3", "日常决策咨询", "消费助手读取该批次数据、关注农场与常购清单，先给结论、再给依据、再给下一步动作"],
        ["S4", "出现质量疑问", "售后助手提交投诉，落库带入 consumer_id，承诺 24 小时首次响应；响应超时由定时任务置 overdue 并升级管理员"],
        ["S5", "农户经营信任者", "农户用邀请码分享给老客户，消费者经 /invite/{code} 注册后自动绑定 inviter，形成可统计的信任者台账"],
    ], [0.08, 0.30, 0.62])))

    A(("h2", "2.2 功能性需求"))
    A(("table", ("表 2-2  功能性需求清单", ["编号", "需求", "验收口径"], [
        ["FR-1", "批次溯源与可信评估", "任一有效溯源码可在 1 秒内返回批次、农事、检测、流转与五维评分，评分项可展开查看证据"],
        ["FR-2", "信任者账户", "手机号注册/登录合一，重复注册自动复用账户；邀请码解析到农户或批次"],
        ["FR-3", "关系与订阅", "关注/取关、常购订阅（含周期与到期日）、行为事件写入，首页按关注与到期日聚合待办"],
        ["FR-4", "AI 消费助手", "支持该批次事实问答、通用知识问答、个人清单问答；回答附工具步骤与知识来源"],
        ["FR-5", "多模态识别", "消费者可上传实物照片，系统先描述可见内容再回答，识别不清晰时明确说明"],
        ["FR-6", "售后与承诺", "投诉带 SLA 时限、首次响应时间与超时标记；农户承诺与履约记录可查询"],
        ["FR-7", "多端工作台与大屏", "农户、经销商、管理员三类角色登录后进入各自工作台，管理员另有全屏信任运营大屏"],
    ], [0.10, 0.26, 0.64])))

    A(("h2", "2.3 非功能性需求"))
    A(("bullet", [
        "性能：单次溯源查询以数据库主键与唯一索引为主，目标 P95 小于 300 ms；AI 回答首字返回小于 2 s（流式），完整回答小于 15 s。",
        "可靠性：大模型接口不可用时，助手降级为基于批次数据模板化生成的确定性回复，演示链路不中断。",
        "安全：密钥一律走环境变量注入；AI 输出经安全过滤；会话隔离，信任者接口未注册时返回业务错误而非 401，避免公开页面被强制跳转登录。",
        "可部署：Docker 多阶段构建，一条 compose 命令拉起数据库与应用；上传目录挂载为卷，二维码基础地址可由环境变量覆盖。",
        "可维护：前后端分离、分层清晰，数据库升级脚本幂等可重复执行，避免演示环境重建时手工改表。",
    ]))

    A(("h2", "2.4 需求边界"))
    A(("p", "本期明确不做支付、订单与物流调度。原因有三：一是赛道评分中「AI 技术深度」权重最高，"
            "把工期投在支付链路对总分贡献极小；二是生鲜交易的资质与履约风险超出学生项目的可控范围；"
            "三是「订阅提醒 + 线下交易」已经足以验证信任关系的留存价值，支付属于后续商业化阶段的工程。"))

    # ---------------- 第三章 ----------------
    A(("pb",))
    A(("h1", "三、系统架构设计"))
    A(("h2", "3.1 总体架构"))
    A(("p", "系统采用前后端分离的单体架构：Spring Boot 负责数据、规则与 AI 编排，Vue 3 单页应用负责交互，"
            "构建产物由后端静态托管，对外只暴露一个端口，便于赛场与云端一键部署。整体分为五层。"))
    A(("table", ("表 3-1  系统分层结构", ["层次", "组成", "职责"], [
        ["接入层", "Nginx/内置容器、/app 静态资源、/api REST、/trace 与 /invite 跳转", "统一入口、路由转发、SPA 与旧链接兼容"],
        ["表现层", "Vue 3 + TypeScript + Element Plus + ECharts", "四端工作台、信任者首页与助手、公开溯源与邀请页、数据大屏"],
        ["应用层", "controller（页面跳转与 REST）、service（业务编排）", "角色鉴权、批次与流转、信任者关系、AI 会话与售后"],
        ["领域能力层", "TrustScoreService、AgentService、KnowledgeService、AiSafetyService、ConsumerService、TrustPromiseService", "可信评分、Agent 编排、RAG 检索、内容安全、关系运营、承诺履约"],
        ["数据层", "MySQL 8 + MyBatis、`uploads/` 文件目录", "业务数据、知识库与向量、检测报告与现场图片"],
    ], [0.14, 0.50, 0.36])))

    A(("h2", "3.2 技术选型与理由"))
    A(("p", "选型遵循三条原则：与赛道硬指标对齐、在两周内可交付、演示环境可复现。"))
    A(("table", ("表 3-2  关键技术选型", ["类别", "选型与版本", "选择理由"], [
        ["后端框架", "Spring Boot 4.0.8 / Java 17", "生态成熟、内嵌容器部署简单；团队前期已有若依风格工程经验，迁移成本低"],
        ["持久层", "MyBatis 4.0.1 + MySQL 8", "溯源类查询以联表与条件筛选为主，手写 SQL 比 ORM 更可控，也便于讲解索引设计"],
        ["前端", "Vue 3 + Vite + TypeScript", "赛道要求现代前端框架；组合式 API 与类型约束适合多人协作，Vite 构建产物小、冷启动快"],
        ["UI 与图表", "Element Plus + ECharts", "表单与表格组件完整，大屏折线/环形图开箱可用，减少自研样式的时间"],
        ["大模型", "通义千问 qwen-plus", "支持 function calling，中文农事语料表现稳定，OpenAI 兼容接口便于替换"],
        ["向量模型", "text-embedding-v3", "与 qwen 同源同账号，减少密钥与环境管理成本"],
        ["多模态", "qwen3-vl-plus", "可直接消费前端压缩后的 JPEG，识别实物与包装标签，中文输出"],
        ["二维码", "ZXing 3.5.3", "本地生成、不依赖第三方服务，包装打印与批量导出可控"],
        ["部署", "Docker 多阶段构建 + docker-compose", "构建与运行分离，镜像体积小；赛场机器只需 Docker 即可复现"],
    ], [0.14, 0.28, 0.58])))

    A(("h2", "3.3 模块划分"))
    A(("p", "后端按职责划分为 8 个包，代码规模共 63 个 Java 文件、6051 行；前端 44 个 .vue/.ts 文件、5444 行。"
            "公开 REST 接口共 55 个（按控制器统计为 13 + 6 + 11 + 9 + 12 + 4）。"))
    A(("table", ("表 3-3  后端包结构与职责", ["包", "主要内容", "说明"], [
        ["controller", "页面跳转控制器、InviteController、legacy 兼容", "负责跳转与旧链接兼容，业务逻辑下沉到 service"],
        ["controller.api", "ApiAuth / ApiTrace / ApiFarmer / ApiDealer / ApiAdmin / ApiConsumer", "六组 REST，统一返回 ApiResult"],
        ["service", "TrustScore、Agent、Knowledge、AiSafety、Consumer、TrustPromise、各业务服务", "核心业务与 AI 编排"],
        ["mapper", "MyBatis 接口与 XML", "单表与联表查询，统一 selectByXxx 命名"],
        ["entity", "批次、农事、检测、流转、投诉、信任者四表实体", "与数据库字段一一对应，驼峰映射"],
        ["config", "拦截器、WebConfig、ComplaintScheduledTask、PasswordMigrationRunner", "鉴权、静态资源与定时任务"],
        ["common", "ApiResult、ApiException、全局异常处理", "统一响应结构与错误码"],
        ["util", "QRCodeUtil、IP 探测、上传工具", "二维码与局域网地址探测"],
    ], [0.18, 0.46, 0.36])))

    A(("h2", "3.4 数据模型"))
    A(("p", "基础库以「一批一码」为主线：crop_batch 记录批次，farming_log 记录农事，detection_report 记录检测，"
            "trace_record 记录流转，complaint 记录售后，scan_log 记录扫码。本轮改造新增 6 张信任者域的表，"
            "并对 3 张既有表做字段扩展，且全部通过幂等脚本升级，不删除任何历史数据。"))
    A(("table", ("表 3-4  本轮新增数据表", ["表名", "关键字段", "作用"], [
        ["consumer_account", "phone（唯一）、nickname、trust_level、invite_code、inviter_type、inviter_id", "信任者账户，手机号即身份"],
        ["consumer_follow", "consumer_id、target_type(BATCH/FARMER)、target_id、notify_enabled", "关注关系，唯一键防重复关注"],
        ["consumer_subscription", "crop_name、batch_id、qty、cycle_days、next_date、status", "常购订阅与到期提醒"],
        ["consumer_event", "consumer_id、event_type、batch_id、payload", "行为事件流，支撑留存分析"],
        ["trust_promise", "batch_id、promise_type、promise_value、evidence_path、status", "农户对批次的承诺与凭证"],
        ["fulfillment_record", "batch_id、dealer_id、consumer_id、ship_time、arrive_time、quality_flag、aftersale_hours", "履约与售后时效记录"],
    ], [0.24, 0.52, 0.24])))
    A(("p", "既有表扩展：complaint 增加 consumer_id、sla_hours（默认 24）、first_reply_time、overdue；"
            "scan_log 增加 consumer_id、scene（默认 PACKAGE）；crop_batch 增加 on_sale、price、stock、subscribe_open、promise_summary。"
            "这些字段让「扫码」第一次能与具体的人关联，也让批次具备「可售 / 可订阅」的语义。"))
    A(("p", "升级脚本采用「先查 information_schema、再动态执行」的写法：每一段 DDL 都用 COUNT(*) 判断表或列是否存在，"
            "存在则执行 SELECT 1 跳过，不存在才 CREATE 或 ALTER。这样脚本可以反复执行而不会报错，"
            "在答辩现场重建数据库时只需运行一次即可，避免手工改表导致的环境漂移。"))

    A(("h2", "3.5 接口与鉴权设计"))
    A(("p", "所有 REST 统一返回 {\"code\":0/非0,\"msg\":\"\",\"data\":...} 结构。后台管理、农户、经销商沿用原有角色会话；"
            "信任者端使用独立会话键 consumerId 与 consumerPhone，与后台账号体系解耦，互不影响。"))
    A(("p", "一个具体的设计取舍：信任者接口在未注册时不返回 HTTP 401，而是返回业务错误码并在 data 中给出引导文案。"
            "原因是公开溯源页与邀请页也挂载了这些接口，若返回 401，前端 axios 拦截器会直接跳转登录页，"
            "把「扫码看菜」的用户强行拦在登录墙外，转化率归零。这个细节决定了「公开页 → 建档」的漏斗是否成立。"))
    A(("table", ("表 3-5  信任者端核心接口", ["方法", "路径", "说明"], [
        ["POST", "/api/consumer/register", "手机号注册或复用账户，可带邀请码，成功后写会话"],
        ["POST", "/api/consumer/login", "按手机号登录，未注册返回业务提示"],
        ["GET", "/api/consumer/me", "当前信任者信息，未登录返回 data=null"],
        ["GET", "/api/consumer/home", "首页聚合：关注、在生长、待办、当季提示"],
        ["POST", "/api/consumer/follow", "关注批次或农户"],
        ["DELETE", "/api/consumer/follow/{id}", "取消关注（校验归属后删除）"],
        ["POST", "/api/consumer/subscription", "创建常购订阅，自动计算下次到期日"],
        ["GET", "/api/consumer/subscriptions", "进行中的订阅列表"],
        ["POST", "/api/consumer/event", "写入行为事件（浏览、提问、点击）"],
        ["GET", "/api/consumer/aftersales", "我的售后单列表"],
        ["GET", "/api/invite/{code}", "邀请信息：邀请人、平均可信分、等级分布"],
    ], [0.12, 0.38, 0.50])))

    # ---------------- 第四章 ----------------
    A(("pb",))
    A(("h1", "四、AI 技术方案"))
    A(("p", "本章是文档的核心。我们把 AI 能力拆成两个明确的分工：确定性部分交给规则，生成式部分交给大模型。"
            "这样做的直接原因是一次评审反馈——「你们让大模型给菜打分，依据是什么？」"
            "这道问题暴露了把判断权交给模型的根本缺陷：不可复核、不可解释、不可复现。"))

    A(("h2", "4.1 双层架构：规则可信层 + 生成式决策层"))
    A(("table", ("表 4-1  两层职责划分", ["层", "承担者", "输入", "输出特性"], [
        ["可信层", "TrustScoreService（规则引擎，244 行）", "检测报告、农事记录、流转记录、批次时间", "整数分、等级、逐项证据，完全确定、可复算"],
        ["决策层", "AgentService + qwen-plus", "可信层结论、工具返回的结构化数据、RAG 命中知识、用户上下文", "自然语言结论与建议，受提示词约束、必须引用事实"],
    ], [0.12, 0.32, 0.30, 0.26])))
    A(("p", "两层的连接方式是「事实前置」：系统在调用模型之前，先把可信指数、批次信息、命中知识预取为一段结构化的"
            "「事实包」，连同系统提示词一起送入模型，并要求模型只能引用事实包与工具返回中的内容。"
            "这样模型不参与判断，只参与解释。"))

    A(("h2", "4.2 大模型集成方式"))
    A(("p", "集成方式没有引入官方 SDK，而是直接用 Hutool 的 HttpRequest 调用 DashScope 的 OpenAI 兼容端点"
            "（https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions），原因是："
            "兼容协议意味着更换模型只需改配置项；直接构造 messages 数组，便于精确控制工具消息与多轮上下文；"
            "不额外引入依赖，镜像体积与启动时间都不受影响。"))
    A(("p", "请求体包含 model、messages、tools、tool_choice、stream 等字段；messages 由系统提示词 + 最近 20 条会话消息组成，"
            "工具调用结果以 role=tool 与 tool_call_id 回填，保证模型能正确关联调用与结果。"
            "接口超时统一设置为 20 秒，失败时记录原始响应体便于排查。"))

    A(("h2", "4.3 Agent 工作流"))
    A(("p", "Agent 采用「预取 + 工具循环 + 收尾」的三段式流程，最多 5 轮工具调用，避免死循环与 token 失控。"))
    A(("bullet", [
        "预取阶段：先判断问题类型，若是批次事实类问题，直接执行 get_batch_info 等工具得到事实包，减少一次模型往返；若涉及专业知识，再并行执行一次 search_knowledge。",
        "循环阶段：把工具 schema 与历史消息送入模型，模型返回 tool_calls 时逐个执行、回填 role=tool 消息，再进入下一轮；每执行一个工具都通过 ToolSink 向前端推送一条 step 事件。",
        "收尾阶段：达到轮数上限或模型不再请求工具时，追加一条「不要再调用工具，请直接根据以上工具结果回答」的系统提示，强制模型输出最终答案。",
    ]))
    A(("p", "下面这段是工具循环的关键结构（节选自 AgentService）："))
    A(("code", """for (int round = 0; round < MAX_ROUNDS; round++) {
    JSONObject msg = callChat(systemPrompt, history, tools, sink);
    if (msg == null) { reply = FALLBACK; break; }
    JSONArray calls = msg.getJSONArray("tool_calls");
    if (calls == null || calls.isEmpty()) { reply = msg.getStr("content"); break; }
    for (JSONObject call : calls) {
        String name = call.getJSONObject("function").getStr("name");
        String args = call.getJSONObject("function").getStr("arguments");
        String result = executeTool(name, args, batchId, consumerId, toolsUsed, sources, sink);
        history.add(new JSONObject().set("role", "tool")
                .set("tool_call_id", call.getStr("id")).set("content", result));
    }
}"""))
    A(("p", "「可见」是这一版的另一个重点。工具执行的每一步都以 step 事件推送到前端，用户看到的是"
            "「查询了批次基础信息 → 检索了 3 条农事记录 → 命中 2 条知识库条目」这样的过程，"
            "而不是一个突然出现的答案。这既提升了可信感，也是评审现场最容易讲清楚的技术点。"))

    A(("h2", "4.4 工具设计与权限"))
    A(("p", "工具是 Agent 与业务数据的唯一通道，共 8 个，分为「批次事实」与「个人上下文」两类。"
            "个人类工具在 consumerId 为空时直接返回「未登录，无法查看」并跳过查询，避免越权读取他人数据。"))
    A(("table", ("表 4-2  工具清单", ["工具名", "类型", "作用", "触发时机"], [
        ["get_batch_info", "批次事实", "作物、品种、产地、播采日期、状态", "问「这是什么菜/什么时候摘的」"],
        ["get_farming_logs", "批次事实", "农事记录：日期、物料、用量", "问「打过几次药/施了什么肥」"],
        ["get_detection_reports", "批次事实", "检测编号、机构、日期、结论", "问「农残合格吗」"],
        ["get_trace_records", "批次事实", "入库、出库、运输环节与地点", "问「从哪来/经过哪些环节」"],
        ["search_knowledge", "通用知识", "农残限量、安全间隔期、储存与清洗", "问标准限值、保存方法"],
        ["get_my_basket", "个人上下文", "常购清单与到期日", "问「我下次该买什么」"],
        ["get_my_history", "个人上下文", "近期提问、浏览、售后记录", "问「我之前买过什么」"],
        ["recommend_in_season", "个人上下文", "关注农场里当季可预约批次", "问「现在有什么值得买」"],
    ], [0.24, 0.16, 0.36, 0.24])))

    A(("h2", "4.5 RAG 检索增强"))
    A(("p", "知识库现有 102 条知识文档，覆盖农残限量标准、安全间隔期、储存保鲜、清洗处理、品类选购等"
            "消费者真正会问的问题。检索链路完整实现了「入库—向量化—召回—引用」四步。"))
    A(("table", ("表 4-3  RAG 处理链路", ["步骤", "实现", "关键参数"], [
        ["入库", "知识文档写库，正文与标题作为向量化文本", "102 条，按 category 分类，记来源与版本"],
        ["向量化", "首次检索时批量调用 embeddings，结果回写数据库 embedding 字段；启动时异步预热，不阻塞启动", "text-embedding-v3，超时 20 s"],
        ["召回", "查询向量化后与缓存的文档向量做余弦相似度，按分数降序取前 3 条", "top_k = 3，阈值 0.32"],
        ["引用", "命中的标题、来源、版本随答案一起返回，前端以「答案依据」展示", "sources 字段"],
    ], [0.12, 0.62, 0.26])))
    A(("p", "工程上有三个细节值得说明。其一，向量缓存以「文档数量」作为失效信号：知识库条数不变则直接复用内存中的向量，"
            "避免每次提问都全表读库；条数变化则重新向量化，兼顾了性能与数据新鲜度。"
            "其二，设置了 0.32 的相关度门槛，低于门槛的片段一律不返回，宁可让模型说「知识库没有这条」，"
            "也不让它拿无关内容凑答案。其三，向量化失败或无密钥时自动降级为关键词匹配，"
            "保证在断网或配额用尽的演示环境下依然可用。"))

    A(("h2", "4.6 多模态识别"))
    A(("p", "多模态解决的是一个非常具体的消费场景：消费者拿着一把菜，想知道「这个能吃吗、怎么洗」。"
            "前端在 AiChat 组件中提供相机与相册入口，图片先在浏览器端用 canvas 压缩为质量 0.8 的 JPEG dataURL，"
            "避免大图直接上传造成的延迟；后端调用 qwen3-vl-plus 得到可见内容的描述，"
            "再以「【图片识别结果】」标记注入提示词，由同一个 Agent 结合批次数据给出结论。"))
    A(("p", "这里有一条硬约束写进了系统提示词：回答开头必须先如实概括图片中可见的内容，"
            "看不清或非实物照片要直接说明，不得凭想象补全。这样既避免模型对着模糊照片编造，"
            "也让用户能立刻判断「AI 到底看到没看到我这张图」。"))

    A(("h2", "4.7 提示词工程"))
    A(("p", "提示词不是一段散文，而是三条可验收的规则。"))
    A(("bullet", [
        "事实边界：涉及该批次的具体数据必须先调用对应工具；预取数据中没有的内容一律视为「平台未登记」，禁止推测；引用批次号、报告编号必须逐字复制，不得改写。",
        "三段式表达：先给结论（能不能买、合不合格），再给依据（数字必须来自工具返回），数据不足必须明说；最后给一个下一步动作（去溯源页核实、加入常购、发起售后）。",
        "价值中立：禁止使用「无打药记录等于生态/有机种植」这类无法由数据证明的表述，避免把「没有记录」包装成「品质背书」。",
    ]))
    A(("p", "此外还有两条工程约束：会话只保留最近 20 条消息，控制上下文长度与成本；达到 5 轮工具调用后"
            "强制收尾，防止模型在多轮检索中发散。"))

    A(("h2", "4.8 AI 安全"))
    A(("p", "AiSafetyService（85 行）在输出侧做统一过滤：拦截医疗诊断与用药建议、绝对化承诺（如「绝对无农残」）、"
            "以及引导线下私下交易等高风险表述；命中即替换为安全话术并记录日志。"
            "输入侧则依靠提示词的边界约束与工具白名单——模型只能调用已注册的 8 个工具，无法访问任意数据表。"))
    A(("table", ("表 4-4  AI 安全措施", ["风险点", "措施", "落点"], [
        ["编造检测结论", "工具返回为空时明确为「平台未登记」，禁止推测；结论只允许引用返回原文", "系统提示词 + 工具返回结构"],
        ["越权读取他人数据", "个人类工具校验 consumerId，为空直接降级", "executeTool"],
        ["医疗与绝对化表述", "输出侧关键词与规则拦截", "AiSafetyService"],
        ["密钥泄露", "api-key 由环境变量注入，代码与文档不含明文", "application.yml + 部署文档"],
        ["服务不可用", "接口失败降级为基于批次数据的确定性回复，并提示稍后再试", "AgentService 兜底分支"],
    ], [0.22, 0.52, 0.26])))

    A(("h2", "4.9 成本与效果控制"))
    A(("p", "赛场环境带宽与配额都有限，因此我们做了四项控制：预取减少往返、历史截断控制上下文、"
            "top-k 与阈值控制检索噪声、结果缓存避免重复向量化。实测一次典型问答的工具调用为 1 至 3 次，"
            "首字返回在 1 秒左右，完整回答 4 至 8 秒，满足现场演示节奏。"))

    # ---------------- 第五章 ----------------
    A(("pb",))
    A(("h1", "五、核心功能实现"))
    A(("h2", "5.1 可信指数与证据链"))
    A(("p", "可信指数由五个维度加权构成，总分 100：第三方检测 30 分、农事记录完整度 20 分、"
            "农药安全间隔期 20 分、流转链路完整性 20 分、数据新鲜度 10 分。"
            "总分不低于 80 判为 GREEN（可信），60 至 79 判为 YELLOW（基本可信），低于 60 判为 RED（需谨慎）。"))
    A(("table", ("表 5-1  五维评分规则", ["维度", "分值", "判定规则"], [
        ["第三方检测", "30", "存在结论为「合格」的报告得 30；有报告但均不合格得 5；无报告得 0 并提示无法核验"],
        ["农事记录完整度", "20", "播种、施肥、打药、灌溉四类各 5 分，缺失项在证据中逐条列出"],
        ["安全间隔期", "20", "最后打药距采收（未采收则距今日）≥7 天得满分；3 至 6 天得 12；小于 3 天或采收后仍打药得 0"],
        ["流转链路完整性", "20", "采收 8 分、入库 6 分、出库或销售 6 分；种植期无流转按 YELLOW 处理"],
        ["数据新鲜度", "10", "最近记录在 7 天内得 10；30 天内得 7；90 天内得 4；更久得 2"],
    ], [0.22, 0.10, 0.68])))
    A(("p", "每个维度不仅输出分数，还输出证据列表，例如「检测报告 NY-2026-0312 · 省农科院 · 2026-09-18 · 结论：合格」"
            "「最后打药 2026-09-02，距采收 12 天」。前端以折叠面板呈现，用户点击即可核对原始记录。"
            "这一点是本项目与「让 AI 打分」类方案的根本差异：分数可以被质疑，证据可以被核实。"))

    A(("h2", "5.2 信任者账户与关系运营"))
    A(("p", "账户体系刻意做得极轻：手机号即身份，不设密码、不设验证码，注册与登录合并为一个动作。"
            "这样的设计在数据安全上确实更宽松，但对一个以「扫码即建档」为生命线的消费场景而言是必要的——"
            "多一步输入，转化就少一截。生产环境应补充短信校验，这一点在第八章的不足中明确列出。"))
    A(("p", "邀请码解析遵循三级优先级：先按后台账号 username 匹配（农户分享自己的账号名作为邀请码），"
            "再按批次的 traceCode 匹配，最后按 batchNo 兜底。命中后写入 inviter_type 与 inviter_id，"
            "形成「谁带来了谁」的关系边，这也是后续按团长分佣与信任单元复制的数据基础。"))
    A(("table", ("表 5-2  关系运营数据流", ["动作", "写入", "用途"], [
        ["注册/登录", "consumer_account、consumer_event(REGISTER/LOGIN)", "账户基数与活跃度"],
        ["关注", "consumer_follow、consumer_event(FOLLOW)", "首页「正在生长」与消息触达"],
        ["订阅", "consumer_subscription、consumer_event(SUBSCRIBE)", "到期提醒与复购引导"],
        ["售后", "complaint(consumer_id)、consumer_event", "服务闭环与超时督办"],
        ["扫码", "scan_log(consumer_id, scene)", "区分「包装扫码」与「分享链接」真实触达"],
    ], [0.16, 0.54, 0.30])))

    A(("h2", "5.3 消费助手"))
    A(("p", "助手页是产品的门面，界面分四区：顶部品牌与信任等级、左侧「我的常购清单」与「关注动态」、"
            "中部对话流、底部输入区。对话流中的每条回答都附带两样东西——工具执行步骤与答案依据，"
            "这两样正是「AI 可信」的外化。"))
    A(("p", "首页聚合接口返回四组数据：follows（关注清单）、growing（关注且处于种植或已采收的批次）、"
            "todos（今日到期的订阅与未关闭的售后单）、tip（当季提示）。前端把 todos 渲染成待办卡片，"
            "把 growing 渲染成成长进度，把未注册用户的首页渲染成「注册引导 + 四步说明」，而不是空白页。"))

    A(("h2", "5.4 承诺与履约"))
    A(("p", "「信任」如果只是形容词，就无法运营。我们把农户的承诺结构化：trust_promise 记录承诺类型"
            "（如不用违禁农药、按时发货、坏果包赔）、承诺内容与凭证图片；fulfillment_record 记录发货时间、"
            "到达时间、质量标记与售后时长。消费端在邀请页与溯源页展示「敢承诺什么」。"
            "承诺一旦写入，就成为农户自己的约束与消费者的核对依据。"))

    A(("h2", "5.5 售后与 SLA"))
    A(("p", "投诉从「匿名留言」升级为「带时限的服务工单」：complaint 新增 consumer_id、sla_hours（默认 24）、"
            "first_reply_time、overdue。ComplaintScheduledTask 定时扫描未首次回复且已超时的工单，"
            "置 overdue 并升级给管理员；消费端在售后查询页直接显示「承诺 24 小时内首次回复」。"
            "把服务承诺写进数据库与定时任务，而不是写在文案里，是这一版最有「产品感」的改动。"))

    A(("h2", "5.6 多端工作台与信任运营大屏"))
    A(("p", "农户端管理批次、农事、检测报告、溯源码与销售去向；经销商端管理入库、出库、流转与来源统计；"
            "管理员端治理用户、批次、投诉与扫码统计，并可进入全屏大屏。大屏在原有数据可视化的基础上"
            "增加了信任者维度：信任者规模、关注转化、订阅到期、售后超时，把「运营」而不只是「监管」摆到台面上。"))

    A(("h2", "5.7 二维码与入口兼容"))
    A(("p", "二维码由 ZXing 本地生成，链接基于应用基础地址拼装。为避免换 WiFi 或热点导致二维码失效，"
            "系统会自动探测当前局域网 IP，探测失败时才回退到 APP_BASE_URL 配置。"
            "同时保留了 /trace/{code}、/qrcode、/app 三个跳转入口——包装已经印出去的码不能作废，"
            "这是任何时候都不能删的三条路由。"))

    # ---------------- 第六章 ----------------
    A(("pb",))
    A(("h1", "六、测试与部署"))
    A(("h2", "6.1 测试策略"))
    A(("p", "测试分三层：单元测试覆盖纯逻辑（密码、二维码、登录拦截），集成测试覆盖上下文启动与数据源连通，"
            "手工链路测试覆盖五个业务场景。当前仓库有 4 个测试类，Maven 构建时随应用一起执行。"))
    A(("table", ("表 6-1  测试覆盖", ["测试类", "覆盖对象", "断言要点"], [
        ["NongyeApplicationTests", "Spring 上下文与数据源", "上下文可加载、Bean 可注入"],
        ["LoginInterceptorTest", "登录拦截器", "未登录访问受保护路径被拦截，公开路径放行"],
        ["PasswordEncoderTest", "BCrypt 编码与迁移", "同一明文两次加密结果不同，校验通过"],
        ["QRCodeUtilTest", "二维码生成", "输出字节非空、可被解码回原文"],
    ], [0.34, 0.26, 0.40])))
    A(("p", "手工链路按 S1 至 S5 逐条走查，重点验证三件事：升级脚本在空库与已有库上都能执行；"
            "无 API Key 时助手仍能给出确定性回复；信任者未注册时公开页不跳登录。"))

    A(("h2", "6.2 部署架构"))
    A(("p", "仓库提供 Dockerfile（多阶段构建：Node 构建前端 → Maven 打包 → JRE 运行）与 docker-compose.yml，"
            "以及 docker/maven-settings.xml 用于国内镜像加速。运行镜像只带 JRE 与 jar，体积显著小于构建镜像。"))
    A(("table", ("表 6-2  关键运行配置", ["配置项", "默认值", "说明"], [
        ["server.port", "8080", "单端口同时提供 API 与 SPA"],
        ["spring.datasource.url", "jdbc:mysql://localhost:3306/nongyeplus", "utf8mb4、东八区、关闭 SSL 校验"],
        ["DB_USERNAME / DB_PASSWORD", "root / 123456", "由环境变量覆盖，禁止用于生产"],
        ["upload.path", "./uploads/", "检测报告、现场图、二维码，容器中挂载为卷"],
        ["app.base-url", "http://localhost:8080", "APP_BASE_URL 覆盖；优先自动探测局域网 IP"],
        ["ai.api-key", "空", "AI_API_KEY 注入；为空时走模拟回复"],
        ["ai.agent-model", "qwen-plus", "支持 function calling"],
        ["ai.vl-model", "qwen3-vl-plus", "图片识别"],
        ["ai.embedding-model", "text-embedding-v3", "RAG 向量化"],
    ], [0.28, 0.34, 0.38])))

    A(("h2", "6.3 启动流程"))
    A(("p", "五步即可复现：导入基础库与信任者升级脚本、配置环境变量、启动后端、构建前端产物到 static/app、"
            "访问 /app 登录。若使用容器，则只需 compose 拉起数据库与应用，再执行两个 SQL 脚本。"))
    A(("code", """# 1. database
mysql -uroot -p nongyeplus < sql/nongyeplus.sql
mysql -uroot -p nongyeplus < sql/upgrade_ai_evidence.sql
mysql -uroot -p nongyeplus < sql/ai_knowledge.sql
mysql -uroot -p nongyeplus < sql/upgrade_trust_consumer.sql

# 2. backend
export AI_API_KEY=sk-****
mvn -q spring-boot:run

# 3. frontend (build into backend static dir)
cd frontend && npm install && npm run build

# or all-in-one
docker compose up -d --build"""))

    A(("h2", "6.4 安全与合规"))
    A(("p", "已落实的措施包括：密钥环境变量化、密码 BCrypt 存储、上传类型与大小限制（单文件 10 MB、单请求 50 MB）、"
            "角色路由守卫、信任者数据按 consumerId 隔离、AI 输出安全过滤。待补强项：短信验证码、接口限流、"
            "图片存储的访问控制、隐私政策与授权说明。"))

    # ---------------- 第七章 ----------------
    A(("pb",))
    A(("h1", "七、团队分工与进度"))
    A(("h2", "7.1 分工"))
    A(("table", ("表 7-1  团队分工", ["成员", "主要职责", "对应交付物"], [
        ["程俊", "前端架构与页面实现（信任者首页、助手页、邀请页、溯源页、大屏）、组件与交互打磨、演示视频脚本与录制",
         "frontend/src 全部页面与组件、演示视频 MP4（1080p，5 至 8 分钟，普通话）"],
        ["华天瑞", "后端与数据库（批次、流转、投诉、信任者六表、升级脚本）、AI 服务集成（Agent/RAG/多模态）、"
         "Git 仓库维护、技术文档编写",
         "src/main/java 全部服务与接口、sql 升级脚本、Public 仓库与 README、本技术文档 PDF"],
        ["第三队员", "测试用例与数据准备、演示环境部署与答辩配合（待补充）",
         "测试记录、演示数据集、答辩 PPT"],
    ], [0.14, 0.56, 0.30])))
    A(("p", "说明：赛事要求以团队形式报名，成员通常为 3 人。当前已明确的两位成员职责如上，"
            "第三位成员需在报名截止前补齐，否则按队伍规模核验时存在风险。另外建议在仓库的提交记录中"
            "体现两人的真实协作痕迹（分支、提交信息、代码归属），这与「完整 Git 记录」的准入要求直接相关。"))

    A(("h2", "7.2 进度安排"))
    A(("table", ("表 7-2  赛前收口计划", ["时间", "目标", "验收"], [
        ["11.09 - 11.10", "补齐消费场景知识库、溯源页信任关系条与下一步动作", "问答命中消费类知识；溯源页有三按钮"],
        ["11.10 - 11.11", "助手接入拍照提问，农户承诺录入页", "助手页可上传图片；承诺可由农户维护"],
        ["11.11 - 11.12", "演示数据准备：含「缺检测」批次、信任者账号、订阅到期、超时售后", "五个场景均可现场复现"],
        ["11.13 - 11.15", "README、仓库公开、提交整理、技术文档定稿", "仓库含 README 与连续提交记录；PDF 不超过 30 页"],
        ["11.15 - 11.17", "演示视频录制与彩排、部署演练", "视频 5 至 8 分钟 1080p 普通话；全流程无卡点"],
        ["11.18 前", "材料提交", "按官方通知逐一核对清单"],
    ], [0.18, 0.52, 0.30])))

    A(("h2", "7.3 材料清单"))
    A(("bullet", [
        "Public 仓库（GitHub 或 Gitee），含 README、目录说明、运行方式、环境变量说明与完整 Git 历史。",
        "技术文档 PDF（不超过 30 页），含需求分析、系统架构、AI 技术方案（选型理由与集成方式）、测试与部署、总结。",
        "演示视频 MP4，1080p，5 至 8 分钟，普通话讲解，覆盖五个业务场景与 AI 能力。",
        "在线演示地址（可选加分项）：公网可访问的部署实例，需保证评审期间稳定。",
    ]))

    # ---------------- 第八章 ----------------
    A(("pb",))
    A(("h1", "八、总结与展望"))
    A(("h2", "8.1 已完成的工作"))
    A(("p", "本期完成了从「大众化溯源平台」到「信任者消费助手」的结构性改造：新增 6 张信任者域表与 11 个字段扩展，"
            "落地 11 个信任者 REST 接口，前端新增首页、助手页、邀请页并改造溯源页与售后页，"
            "Agent 从单轮问答升级为 8 工具、5 轮编排、可流式观察的智能体，知识库扩充至 102 条并接入向量检索，"
            "售后增加 SLA 与超时督办。技术上形成了「规则算分、模型解释」的双层结构，"
            "产品上形成了「建档—关注—提醒—复购—售后」的闭环。"))

    A(("h2", "8.2 存在的不足"))
    A(("bullet", [
        "身份校验偏弱：当前以手机号直接建档，缺少短信验证，存在被冒用的可能，生产环境必须补齐。",
        "履约数据依赖经销商录入，实际覆盖率不足时，承诺兑现率会失去参考意义。",
        "多模态目前只在溯源页浮窗生效，尚未接入助手主路径，用户路径还不够顺。",
        "知识库缺少消费场景语料（如「这个菜怎么做」「孕妇能吃吗」），RAG 命中率仍有提升空间。",
        "代码仓库仍有历史遗留的 Thymeleaf 页面与旧控制器未清理，属于技术债，计划赛后处理。",
    ]))

    A(("h2", "8.3 后续规划"))
    A(("p", "短期（赛前）：补齐消费场景知识、把多模态前置到助手、增加农户承诺录入与履约看板、"
            "整理 README 与提交记录，确保材料准入项全部达标。"))
    A(("p", "中期（赛后一个版本）：引入短信验证与接口限流，把助手改造成「主动提醒」形态——"
            "订阅到期、批次进入采收、售后超时都由助手主动推送，而不是等用户提问。"))
    A(("p", "长期（商业化）：以「信任运营」为收费点，向农户与合作社提供信任者台账、承诺管理与检测导流服务；"
            "以社区团长为增长单元，按带来的信任者规模结算分佣；消费端持续免费，"
            "用免费换规模、用服务换收入。这是我们理解的、属于这个项目最现实的一条商业路径。"))
    A(("note", "（文档完）"))
    return B


# ============================================================
# DOCX 渲染
# ============================================================
def render_docx(blocks):
    from docx import Document
    from docx.shared import Pt, Cm, RGBColor
    from docx.enum.text import WD_ALIGN_PARAGRAPH, WD_LINE_SPACING
    from docx.enum.table import WD_TABLE_ALIGNMENT
    from docx.oxml.ns import qn
    from docx.oxml import OxmlElement

    CN_BODY = "宋体"
    CN_HEAD = "黑体"

    doc = Document()
    sec = doc.sections[0]
    sec.page_width, sec.page_height = Cm(21), Cm(29.7)
    sec.left_margin = sec.right_margin = Cm(2.5)
    sec.top_margin = Cm(2.6)
    sec.bottom_margin = Cm(2.4)

    st = doc.styles["Normal"]
    st.font.name = CN_BODY
    st.font.size = Pt(10.5)
    st.element.rPr.rFonts.set(qn("w:eastAsia"), CN_BODY)
    st.paragraph_format.line_spacing = 1.5
    st.paragraph_format.space_after = Pt(2)
    st.paragraph_format.first_line_indent = Cm(0.74)

    def set_font(run, name, size=None, bold=False, color=None):
        run.font.name = name
        run._element.rPr.rFonts.set(qn("w:eastAsia"), name)
        if size: run.font.size = Pt(size)
        run.font.bold = bold
        if color: run.font.color.rgb = color

    def add_page_number(par):
        run = par.add_run()
        fld = OxmlElement("w:fldSimple")
        fld.set(qn("w:instr"), "PAGE")
        par._p.append(fld)

    # 页眉页脚
    hdr = sec.header.paragraphs[0]
    hdr.alignment = WD_ALIGN_PARAGRAPH.RIGHT
    r = hdr.add_run("安溯·知源 VeriTrace 技术文档")
    set_font(r, CN_BODY, 9, color=RGBColor(0x88, 0x88, 0x88))
    ftr = sec.footer.paragraphs[0]
    ftr.alignment = WD_ALIGN_PARAGRAPH.CENTER
    r = ftr.add_run("第 ")
    set_font(r, CN_BODY, 9)
    add_page_number(ftr)
    r = ftr.add_run(" 页")
    set_font(r, CN_BODY, 9)

    # 封面
    for _ in range(3):
        doc.add_paragraph()
    p = doc.add_paragraph(); p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    r = p.add_run(TITLE); set_font(r, CN_HEAD, 30, bold=True)
    p.paragraph_format.space_after = Pt(6)
    p = doc.add_paragraph(); p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    r = p.add_run(SUBTITLE); set_font(r, CN_BODY, 15)
    p = doc.add_paragraph(); p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    r = p.add_run(TEAM_LINE); set_font(r, CN_BODY, 12, color=RGBColor(0x55, 0x55, 0x55))

    for _ in range(6):
        doc.add_paragraph()

    info = [("项目名称", "安溯·知源 VeriTrace —— 农产品可信溯源与信任者消费助手"),
            ("参赛赛道", "AI WEB 网页开发挑战赛"),
            ("技术栈", "Spring Boot 4.0.8 / Java 17 / MyBatis / MySQL 8 / Vue 3 + Vite + TypeScript / 通义千问"),
            ("团队成员", "程俊、华天瑞、（第三位成员待补）"),
            ("提交日期", DATE_LINE)]
    tb = doc.add_table(rows=0, cols=2)
    tb.alignment = WD_TABLE_ALIGNMENT.CENTER
    for k, v in info:
        cells = tb.add_row().cells
        cells[0].width, cells[1].width = Cm(3.2), Cm(11.5)
        pr = cells[0].paragraphs[0]; pr.alignment = WD_ALIGN_PARAGRAPH.RIGHT
        rr = pr.add_run(k); set_font(rr, CN_HEAD, 10.5)
        pv = cells[1].paragraphs[0]
        rv = pv.add_run(v); set_font(rv, CN_BODY, 10.5)
    doc.add_page_break()

    # 目录
    p = doc.add_paragraph(); p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    r = p.add_run("目  录"); set_font(r, CN_HEAD, 16, bold=True)
    p.paragraph_format.space_after = Pt(12)
    toc_par = doc.add_paragraph()
    fld = OxmlElement("w:fldSimple")
    fld.set(qn("w:instr"), r'TOC \o "1-2" \h \z \u')
    inner_p = OxmlElement("w:p")
    inner_r = OxmlElement("w:r")
    inner_t = OxmlElement("w:t")
    inner_t.text = "（在 Word 中右键此处选择「更新域」即可生成目录）"
    inner_r.append(inner_t); inner_p.append(inner_r); fld.append(inner_p)
    toc_par._p.append(fld)

    settings = doc.settings.element
    upd = OxmlElement("w:updateFields")
    upd.set(qn("w:val"), "true")
    settings.append(upd)

    def shade(cell, hexcolor):
        tcPr = cell._tc.get_or_add_tcPr()
        sh = OxmlElement("w:shd")
        sh.set(qn("w:val"), "clear"); sh.set(qn("w:color"), "auto"); sh.set(qn("w:fill"), hexcolor)
        tcPr.append(sh)

    def set_borders(table):
        tbl = table._tbl
        tblPr = tbl.tblPr
        borders = OxmlElement("w:tblBorders")
        for edge in ("top", "left", "bottom", "right", "insideH", "insideV"):
            e = OxmlElement(f"w:{edge}")
            e.set(qn("w:val"), "single"); e.set(qn("w:sz"), "6")
            e.set(qn("w:space"), "0"); e.set(qn("w:color"), "999999")
            borders.append(e)
        tblPr.append(borders)

    for blk in blocks:
        kind = blk[0]
        if kind == "h1":
            doc.add_page_break()
            p = doc.add_paragraph()
            p.paragraph_format.space_before = Pt(6)
            p.paragraph_format.space_after = Pt(10)
            p.paragraph_format.first_line_indent = Cm(0)
            r = p.add_run(blk[1]); set_font(r, CN_HEAD, 17, bold=True)
        elif kind == "h2":
            p = doc.add_paragraph()
            p.paragraph_format.space_before = Pt(10)
            p.paragraph_format.space_after = Pt(4)
            p.paragraph_format.first_line_indent = Cm(0)
            r = p.add_run(blk[1]); set_font(r, CN_HEAD, 13, bold=True)
        elif kind == "h3":
            p = doc.add_paragraph()
            p.paragraph_format.first_line_indent = Cm(0)
            r = p.add_run(blk[1]); set_font(r, CN_HEAD, 11, bold=True)
        elif kind == "p":
            p = doc.add_paragraph()
            p.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY
            r = p.add_run(blk[1]); set_font(r, CN_BODY, 10.5)
        elif kind == "bullet":
            for item in blk[1]:
                p = doc.add_paragraph(style="List Bullet")
                p.paragraph_format.left_indent = Cm(0.74)
                p.paragraph_format.first_line_indent = Cm(0)
                p.paragraph_format.space_after = Pt(2)
                r = p.add_run(item); set_font(r, CN_BODY, 10.5)
        elif kind == "note":
            p = doc.add_paragraph(); p.alignment = WD_ALIGN_PARAGRAPH.CENTER
            r = p.add_run(blk[1]); set_font(r, CN_BODY, 9, color=RGBColor(0x88, 0x88, 0x88))
        elif kind == "code":
            for line in blk[1].split("\n"):
                p = doc.add_paragraph()
                p.paragraph_format.left_indent = Cm(0.6)
                p.paragraph_format.first_line_indent = Cm(0)
                p.paragraph_format.line_spacing = 1.0
                p.paragraph_format.space_after = Pt(0)
                # 段落底色
                pPr = p._p.get_or_add_pPr()
                sh = OxmlElement("w:shd")
                sh.set(qn("w:val"), "clear"); sh.set(qn("w:color"), "auto"); sh.set(qn("w:fill"), "F4F6F4")
                pPr.append(sh)
                r = p.add_run(line if line else " ")
                r.font.name = "Consolas"
                r.font.size = Pt(8.5)
        elif kind == "table":
            cap, header, rows, ratios = blk[1]
            p = doc.add_paragraph(); p.alignment = WD_ALIGN_PARAGRAPH.CENTER
            p.paragraph_format.first_line_indent = Cm(0)
            p.paragraph_format.space_before = Pt(6)
            p.paragraph_format.space_after = Pt(3)
            r = p.add_run(cap); set_font(r, CN_HEAD, 9.5, bold=True)
            total = 16.0
            tb = doc.add_table(rows=1, cols=len(header))
            tb.alignment = WD_TABLE_ALIGNMENT.CENTER
            tb.autofit = False
            set_borders(tb)
            hdr_cells = tb.rows[0].cells
            for i, h in enumerate(header):
                hdr_cells[i].width = Cm(total * ratios[i])
                ph = hdr_cells[i].paragraphs[0]
                ph.alignment = WD_ALIGN_PARAGRAPH.CENTER
                ph.paragraph_format.first_line_indent = Cm(0)
                ph.paragraph_format.line_spacing = 1.0
                rh = ph.add_run(h); set_font(rh, CN_HEAD, 9, bold=True)
                shade(hdr_cells[i], "EAF1EA")
            for row in rows:
                cells = tb.add_row().cells
                for i, val in enumerate(row):
                    cells[i].width = Cm(total * ratios[i])
                    pc = cells[i].paragraphs[0]
                    pc.paragraph_format.first_line_indent = Cm(0)
                    pc.paragraph_format.line_spacing = 1.0
                    pc.paragraph_format.space_after = Pt(0)
                    rc = pc.add_run(val); set_font(rc, CN_BODY, 9)
            doc.add_paragraph().paragraph_format.space_after = Pt(4)

    doc.save(DOCX_PATH)
    return DOCX_PATH


# ============================================================
# PDF 渲染
# ============================================================
def render_pdf(blocks):
    from reportlab.lib.pagesizes import A4
    from reportlab.lib.units import cm, mm
    from reportlab.lib import colors
    from reportlab.lib.styles import ParagraphStyle
    from reportlab.lib.enums import TA_JUSTIFY, TA_CENTER, TA_LEFT
    from reportlab.pdfbase import pdfmetrics
    from reportlab.pdfbase.ttfonts import TTFont
    from reportlab.platypus import (BaseDocTemplate, PageTemplate, Frame, Paragraph,
                                    Spacer, Table, TableStyle, PageBreak, Preformatted,
                                    KeepTogether)
    from reportlab.platypus.tableofcontents import TableOfContents
    from reportlab.pdfbase.ttfonts import TTFont as RLTTFont
    from fontTools.ttLib import TTFont as FTFont

    FONT_DIR = r"C:\Windows\Fonts"
    tmp_dir = os.path.join(OUT_DIR, "_fonts")
    os.makedirs(tmp_dir, exist_ok=True)
    song_ttf = os.path.join(tmp_dir, "simsun.ttf")
    if not os.path.exists(song_ttf):
        f = FTFont(os.path.join(FONT_DIR, "simsun.ttc"), fontNumber=0)
        f.save(song_ttf)
    pdfmetrics.registerFont(RLTTFont("SongTi", song_ttf))
    pdfmetrics.registerFont(RLTTFont("HeiTi", os.path.join(FONT_DIR, "simhei.ttf")))
    pdfmetrics.registerFont(RLTTFont("Mono", os.path.join(FONT_DIR, "consola.ttf")))
    pdfmetrics.registerFontFamily("SongTi", normal="SongTi", bold="HeiTi",
                                  italic="SongTi", boldItalic="HeiTi")

    GREEN = colors.HexColor("#2E5D46")

    body = ParagraphStyle("body", fontName="SongTi", fontSize=10.5, leading=19,
                          firstLineIndent=21, alignment=TA_JUSTIFY, spaceAfter=5,
                          wordWrap="CJK", textColor=colors.HexColor("#1A1A1A"))
    h1s = ParagraphStyle("h1", fontName="HeiTi", fontSize=17, leading=24,
                         spaceBefore=2, spaceAfter=10, textColor=GREEN)
    h2s = ParagraphStyle("h2", fontName="HeiTi", fontSize=13, leading=19,
                         spaceBefore=10, spaceAfter=4, textColor=colors.HexColor("#22332A"))
    cap = ParagraphStyle("cap", fontName="HeiTi", fontSize=9.5, leading=14,
                         alignment=TA_CENTER, spaceBefore=6, spaceAfter=4)
    cell = ParagraphStyle("cell", fontName="SongTi", fontSize=8.2, leading=11.5, wordWrap="CJK")
    cellh = ParagraphStyle("cellh", fontName="HeiTi", fontSize=8.4, leading=11.5, wordWrap="CJK")
    codes = ParagraphStyle("code", fontName="Mono", fontSize=7.8, leading=10.5,
                           textColor=colors.HexColor("#1F2A22"))
    bullets = ParagraphStyle("bul", parent=body, firstLineIndent=0, leftIndent=16,
                             bulletIndent=4, spaceAfter=4)
    notes = ParagraphStyle("note", fontName="SongTi", fontSize=9, leading=14,
                           alignment=TA_CENTER, textColor=colors.HexColor("#888888"))

    def esc(t):
        return t.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

    class Doc(BaseDocTemplate):
        def __init__(self, fn, **kw):
            BaseDocTemplate.__init__(self, fn, **kw)
            frame = Frame(2.5 * cm, 2.2 * cm, A4[0] - 5 * cm, A4[1] - 4.6 * cm, id="main")
            self.addPageTemplates([PageTemplate(id="cover", frames=[frame], onPage=self.cover_page),
                                   PageTemplate(id="body", frames=[frame], onPage=self.body_page)])
            self.show_chapter = False

        def cover_page(self, canv, doc):
            pass

        def body_page(self, canv, doc):
            canv.saveState()
            canv.setFont("SongTi", 8.5)
            canv.setFillColor(colors.HexColor("#8A8A8A"))
            canv.drawRightString(A4[0] - 2.5 * cm, A4[1] - 1.6 * cm, "安溯·知源 VeriTrace 技术文档")
            canv.setStrokeColor(colors.HexColor("#D8E2D8"))
            canv.setLineWidth(0.5)
            canv.line(2.5 * cm, A4[1] - 1.75 * cm, A4[0] - 2.5 * cm, A4[1] - 1.75 * cm)
            canv.setFont("SongTi", 9)
            canv.setFillColor(colors.HexColor("#666666"))
            canv.drawCentredString(A4[0] / 2, 1.3 * cm, "第 %d 页" % (doc.page - 1))
            canv.restoreState()

        def afterFlowable(self, flowable):
            if isinstance(flowable, Paragraph):
                sk = flowable.style.name
                txt = flowable.getPlainText()
                if sk == "h1":
                    self.show_chapter = True
                    self.notify("TOCEntry", (0, txt, self.page - 1))
                elif sk == "h2":
                    self.notify("TOCEntry", (1, txt, self.page - 1))

    story = []

    # 封面
    story.append(Spacer(1, 3.2 * cm))
    story.append(Paragraph(TITLE, ParagraphStyle("t", fontName="HeiTi", fontSize=30,
                                                 leading=40, alignment=TA_CENTER,
                                                 textColor=colors.HexColor("#1F3A2B"))))
    story.append(Spacer(1, 0.5 * cm))
    story.append(Paragraph(SUBTITLE, ParagraphStyle("s", fontName="SongTi", fontSize=15,
                                                    leading=24, alignment=TA_CENTER)))
    story.append(Spacer(1, 0.9 * cm))
    story.append(Paragraph(TEAM_LINE, ParagraphStyle("t2", fontName="SongTi", fontSize=11.5,
                                                    leading=18, alignment=TA_CENTER,
                                                    textColor=colors.HexColor("#555555")))
    )
    story.append(Spacer(1, 7.2 * cm))
    info_rows = [
        ["项目名称", "安溯·知源 VeriTrace —— 农产品可信溯源与信任者消费助手"],
        ["参赛赛道", "AI WEB 网页开发挑战赛"],
        ["技术栈", "Spring Boot 4.0.8 / Java 17 / MyBatis / MySQL 8 / Vue 3 + Vite + TypeScript / 通义千问"],
        ["团队成员", "程俊、华天瑞、（第三位成员待补）"],
        ["提交日期", DATE_LINE],
    ]
    info_data = [[Paragraph(esc(k), ParagraphStyle("k", fontName="HeiTi", fontSize=10, leading=16, alignment=2)),
                  Paragraph(esc(v), ParagraphStyle("v", fontName="SongTi", fontSize=10, leading=16, wordWrap="CJK"))]
                 for k, v in info_rows]
    t = Table(info_data, colWidths=[3.0 * cm, 11.0 * cm], hAlign="CENTER")
    t.setStyle(TableStyle([("VALIGN", (0, 0), (-1, -1), "MIDDLE"),
                           ("TOPPADDING", (0, 0), (-1, -1), 3),
                           ("BOTTOMPADDING", (0, 0), (-1, -1), 3)]))
    story.append(t)
    story.append(PageBreak())

    # 目录页
    story.append(Paragraph("目　录", ParagraphStyle("toct", fontName="HeiTi", fontSize=16,
                                                    leading=22, alignment=TA_CENTER, spaceAfter=14)))
    toc = TableOfContents()
    toc.levelStyles = [
        ParagraphStyle("TOC1", fontName="HeiTi", fontSize=11, leading=20, leftIndent=0),
        ParagraphStyle("TOC2", fontName="SongTi", fontSize=9.8, leading=17, leftIndent=18),
    ]
    toc.dotsMinLevel = 0
    story.append(toc)

    first_h1 = True
    for blk in blocks:
        kind = blk[0]
        if kind == "h1":
            if not first_h1:
                story.append(PageBreak())
            first_h1 = False
            story.append(Paragraph(esc(blk[1]), h1s))
        elif kind == "h2":
            story.append(Paragraph(esc(blk[1]), h2s))
        elif kind == "p":
            story.append(Paragraph(esc(blk[1]), body))
        elif kind == "bullet":
            for item in blk[1]:
                story.append(Paragraph(esc(item), bullets, bulletText="•"))
        elif kind == "note":
            story.append(Spacer(1, 10))
            story.append(Paragraph(esc(blk[1]), notes))
        elif kind == "code":
            lines = blk[1].split("\n")
            pre = Preformatted("\n".join(lines), codes)
            t = Table([[pre]], colWidths=[16 * cm])
            t.setStyle(TableStyle([("BACKGROUND", (0, 0), (-1, -1), colors.HexColor("#F4F6F4")),
                                   ("BOX", (0, 0), (-1, -1), 0.4, colors.HexColor("#DDE5DD")),
                                   ("LEFTPADDING", (0, 0), (-1, -1), 6),
                                   ("RIGHTPADDING", (0, 0), (-1, -1), 6),
                                   ("TOPPADDING", (0, 0), (-1, -1), 5),
                                   ("BOTTOMPADDING", (0, 0), (-1, -1), 5)]))
            story.append(Spacer(1, 3))
            story.append(t)
            story.append(Spacer(1, 7))
        elif kind == "table":
            caption, header, rows, ratios = blk[1]
            story.append(Paragraph(esc(caption), cap))
            data = [[Paragraph(esc(h), cellh) for h in header]]
            for row in rows:
                data.append([Paragraph(esc(v), cell) for v in row])
            widths = [16 * cm * r for r in ratios]
            t = Table(data, colWidths=widths, repeatRows=1, hAlign="CENTER")
            t.setStyle(TableStyle([
                ("GRID", (0, 0), (-1, -1), 0.4, colors.HexColor("#9FB2A4")),
                ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#EAF1EA")),
                ("VALIGN", (0, 0), (-1, -1), "MIDDLE"),
                ("TOPPADDING", (0, 0), (-1, -1), 3.5),
                ("BOTTOMPADDING", (0, 0), (-1, -1), 3.5),
                ("LEFTPADDING", (0, 0), (-1, -1), 4),
                ("RIGHTPADDING", (0, 0), (-1, -1), 4),
            ]))
            story.append(t)
            story.append(Spacer(1, 9))

    doc = Doc(PDF_PATH, pagesize=A4, title="安溯·知源 VeriTrace 技术文档",
              author="程俊、华天瑞", subject="AI WEB 网页开发挑战赛技术文档")
    doc.multiBuild(story)
    return PDF_PATH, doc.page - 1


if __name__ == "__main__":
    blocks = build_blocks()
    p1 = render_docx(blocks)
    print("DOCX:", p1, os.path.getsize(p1) // 1024, "KB")
    p2, pages = render_pdf(blocks)
    print("PDF:", p2, os.path.getsize(p2) // 1024, "KB", "body pages =", pages)
