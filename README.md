# 安溯·知源 VeriTrace

> AI 驱动可信溯源决策平台 —— 从"扫码看报告"到"消费者长期信任关系"

## 一句话定位

传统溯源解决"信息展示"，**安溯·知源解决"消费决策"**：扫码后不只告诉你"检了什么"，还告诉你**这批菜该不该买、怎么存、出了问题谁负责、下次什么时候该再买**。

## 核心差异化（相对常规"溯源 + AI 问答"项目）

| 能力 | 常规做法 | 本项目做法 |
|---|---|---|
| 可信度评估 | 展示一份检测报告 | **AI 可信指数**：确定性规则引擎五维打分（检测 30 / 农事 20 / 安全间隔期 20 / 流转 20 / 新鲜度 10），红黄绿分级，**每项可展开查看真实证据**，非大模型主观打分 |
| 消费者理解 | 罗列数据表格 | **决策翻译**：把数据翻译成"建议购买 / 可以买注意核对 / 建议谨慎"+ 放心理由 + 风险提示 + 保存建议 |
| 信任来源 | 只有平台背书 | **农户事前承诺 + 系统自动兑现判定**：新鲜承诺（采收→入库耗时）、采收期承诺、品质承诺，按真实流转时间自动判定，不可事后篡改 |
| 售后服务 | 提交投诉后无下文 | **SLA 倒计时 + 超时自动升级**：首次回复时间打点，超时工单标红并进入管理员红色角标，消费者端实时看到处理进度条 |
| AI 助手 | 单批次一次性问答 | **信任者长期上下文**：邀请码注册建档 → 关注农场/批次 → 常购订阅 → 助手注入消费者身份与历史事件，可用工具查询"我常买什么""上次那个批次怎么样" |

## 技术栈

- **后端**：Spring Boot 4 / Java 17 / MyBatis / MySQL 8
- **前端**：Vue 3 + TypeScript（strict）+ Vite + Element Plus + ECharts，构建产物内嵌 `static/app/`，单服务部署
- **AI**：通义千问 `qwen-plus`（function calling Agent，5+3 工具）、`qwen3-vl-plus`（图片识别）、`text-embedding-v3`（RAG 向量检索，126 条消费/标准知识）

## 目录结构

```
src/main/java/org/example/nongye/
├── controller/           页面控制器 + REST（api 子包返回 {code,msg,data}）
│   └── api/              ApiTraceController（公开溯源）/ ApiConsumerController（信任者）
│                         ApiFarmerController / ApiDealerController / ApiAdminController / AiController
├── service/              AgentService（function calling）/ TrustScoreService（确定性评分）
│                         TrustPromiseService（承诺兑现）/ KnowledgeService（RAG）/ AiSafetyService
├── config/               WebConfig / LoginInterceptor / ComplaintScheduledTask（SLA 扫描）
├── entity/ mapper/       数据实体与注解式 Mapper
└── util/                 二维码、局域网 IP 探测等

frontend/src/
├── views/consumer/       溯源详情 / 投诉 / 邀请注册 / 信任者首页 / 消费助手
├── views/farmer|dealer|admin/  三角色管理端页面（含信任承诺录入）
├── components/           AiChat（SSE 流式）/ ConsoleLayout / 图表组件
├── api/ store/ router/   axios 封装 / 会话状态 / 路由
└── types/index.ts        全量业务类型定义

sql/
├── nongyeplus.sql                基础库表与演示数据
├── ai_knowledge.sql              RAG 知识库（标准类 102 条）
├── consumer_knowledge_seed.sql   消费场景知识（储存/食用/选购 24 条）
├── upgrade_trust_consumer.sql    信任者体系表（消费账户/关注/订阅/事件/承诺/履约）
└── trust_promise_seed.sql        演示批次的信任承诺
```

## 快速启动

### 1. 初始化数据库

```bash
mysql -uroot -p123456 -e "CREATE DATABASE IF NOT EXISTS nongyeplus DEFAULT CHARSET utf8mb4;"
mysql -uroot -p123456 nongyeplus < sql/nongyeplus.sql
mysql -uroot -p123456 nongyeplus < sql/upgrade_trust_consumer.sql
mysql -uroot -p123456 nongyeplus < sql/ai_knowledge.sql
mysql -uroot -p123456 nongyeplus < sql/consumer_knowledge_seed.sql
mysql -uroot -p123456 nongyeplus < sql/trust_promise_seed.sql
```

### 2. 启动后端

```bash
export AI_API_KEY=sk-xxxx        # Windows: $env:AI_API_KEY='sk-xxxx'
mvn spring-boot:run
# 指定端口：mvn '-Dspring-boot.run.arguments=--server.port=8081' spring-boot:run
```

未配置 `AI_API_KEY` 时 AI 能力自动降级为规则文案，页面其余功能不受影响。

### 3. 构建前端（修改前端源码后必须执行）

```bash
cd frontend
npm install
npm run type-check     # vue-tsc strict，要求零错误
npm run build          # 输出到 src/main/resources/static/app/
```

## 演示路径

| 角色 | 地址 | 账号 |
|---|---|---|
| 管理员 | `/app/login` | admin / 123456 |
| 农户 | `/app/login` | farmer / 123456 |
| 经销商 | `/app/login` | dealer / 123456 |
| 消费者 | 扫码或 `/app/trace/TZ202603001001` | 无需登录 |

推荐演示顺序：

1. **扫码溯源**：打开 `/app/trace/TZ202603001001` → 看 AI 可信指数（五维 + 证据展开）→ 决策翻译卡（该不该买 / 怎么存）→ 信任承诺兑现 → 生成 AI 体检报告
2. **信任者注册**：访问 `/app/invite/admin` → 手机号 + 昵称注册 → 自动进入信任者首页
3. **消费助手**：首页点"助手一句话"或溯源页点"问 AI 助手" → 提问"我常买什么菜""上次那个西红柿怎么样"，观察工具调用徽章与流式回答
4. **承诺闭环**：农户端登录 → 侧边栏「信任承诺」新增一条承诺 → 回溯源页看到承诺与兑现判定
5. **售后 SLA**：提交投诉 → 投诉查询页查看倒计时 → 管理员端工单超时后标红升级

## 关键设计说明

- **可信指数用规则不用大模型**：可复核、可追责，答辩时能逐项指出证据来源
- **AI 回答必须可核验**：服务端先预取批次/农事/检测/流转/知识库数据再回答，出口过内容安全过滤，未命中知识时提示以包装标签或检测报告原文为准
- **两条 AI 路径融合**：图片先经视觉模型识别，识别结果拼入问题再交给 Agent，而非直接返回视觉结论
- **权限与数据隔离**：消费者接口未注册时返回业务错误而非 401（避免公开页被强制跳登录）；农户只能操作自己名下批次；经销商只能看到经手批次
- **软删除与事务**：查询统一过滤 `deleted=0`，多步写操作加 `@Transactional`

## 团队信息

| 角色 | 姓名 | 分工 |
|---|---|---|
| 队员 | 程俊 | 前端页面、交互演示与演讲视频 |
| 队员 | 华天瑞 | 后端接口、数据库与 Git 仓库 |
| 指导教师 | 满国晶 | 【待补充】 |

## 文档

- 参赛技术文档：`docs/安溯知源-参赛技术文档.pdf`（可编辑源文件 `docs/安溯知源-参赛技术文档.docx`）
- 环境配置与部署说明：`docs/环境配置与部署说明.md`
- 依赖清单：`docs/依赖清单.md`
- 环境变量模板：`.env.example`
- 详细改造方案：[信任者消费助手改造方案.md](./信任者消费助手改造方案.md)
- AI 可信评测脚本：`scripts/ai_eval.ps1`
