# -*- coding: utf-8 -*-
"""生成《安溯·知源 VeriTrace 技术文档》DOCX。"""
import os
from docx import Document
from docx.shared import Pt, Cm, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT
from docx.oxml.ns import qn
from docx.oxml import OxmlElement

ROOT = r"D:\若依\nongye"
OUT = os.path.join(ROOT, "docs", "安溯知源-参赛技术文档.docx")

doc = Document()

# 页面设置 A4 + 2.5cm 边距
sec = doc.sections[0]
sec.page_width = Cm(21.0)
sec.page_height = Cm(29.7)
sec.top_margin = Cm(2.5)
sec.bottom_margin = Cm(2.5)
sec.left_margin = Cm(2.5)
sec.right_margin = Cm(2.5)

# 基础样式
normal = doc.styles["Normal"]
normal.font.name = "Times New Roman"
normal.font.size = Pt(12)
normal._element.rPr.rFonts.set(qn("w:eastAsia"), "宋体")

BLACK = RGBColor(0, 0, 0)


def _set_run(run, cn="宋体", en="Times New Roman", size=12, bold=False, color=BLACK):
    run.font.name = en
    run._element.rPr.rFonts.set(qn("w:eastAsia"), cn)
    run.font.size = Pt(size)
    run.font.bold = bold
    run.font.color.rgb = color


def title(text):
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    r = p.add_run(text)
    _set_run(r, cn="黑体", size=18, bold=True)
    p.paragraph_format.space_after = Pt(18)
    return p


def subtitle(text):
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    r = p.add_run(text)
    _set_run(r, cn="楷体", size=12)
    p.paragraph_format.space_after = Pt(12)
    return p


def h1(text):
    p = doc.add_paragraph(style="Heading 1")
    r = p.add_run(text)
    _set_run(r, cn="黑体", size=16, bold=True)
    p.paragraph_format.space_before = Pt(14)
    p.paragraph_format.space_after = Pt(6)
    p.paragraph_format.keep_with_next = True
    return p


def h2(text):
    p = doc.add_paragraph(style="Heading 2")
    r = p.add_run(text)
    _set_run(r, cn="黑体", size=14, bold=True)
    p.paragraph_format.space_before = Pt(11)
    p.paragraph_format.space_after = Pt(5)
    p.paragraph_format.keep_with_next = True
    return p


def body(text, indent=True):
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY
    r = p.add_run(text)
    _set_run(r, size=12)
    pf = p.paragraph_format
    pf.line_spacing = 1.5
    pf.space_before = Pt(0)
    pf.space_after = Pt(0)
    if indent:
        pf.first_line_indent = Pt(24)
    return p


def bullet(text):
    p = doc.add_paragraph(style="List Bullet")
    r = p.add_run(text)
    _set_run(r, size=12)
    p.paragraph_format.line_spacing = 1.5
    return p


def shade_cell(cell, color="D9D9D9"):
    tcPr = cell._tc.get_or_add_tcPr()
    shd = OxmlElement("w:shd")
    shd.set(qn("w:val"), "clear")
    shd.set(qn("w:fill"), color)
    tcPr.append(shd)


def add_table(headers, rows, widths=None):
    t = doc.add_table(rows=1, cols=len(headers))
    t.alignment = WD_TABLE_ALIGNMENT.CENTER
    t.style = "Table Grid"
    hdr = t.rows[0].cells
    for i, htext in enumerate(headers):
        shade_cell(hdr[i])
        p = hdr[i].paragraphs[0]
        p.alignment = WD_ALIGN_PARAGRAPH.CENTER
        r = p.add_run(htext)
        _set_run(r, cn="黑体", size=10.5, bold=True)
    for row in rows:
        cells = t.add_row().cells
        for i, val in enumerate(row):
            p = cells[i].paragraphs[0]
            p.alignment = WD_ALIGN_PARAGRAPH.LEFT if i == len(row) - 1 else WD_ALIGN_PARAGRAPH.CENTER
            r = p.add_run(str(val))
            _set_run(r, size=10.5)
    if widths:
        for i, w in enumerate(widths):
            for row in t.rows:
                row.cells[i].width = Cm(w)
    spacer = doc.add_paragraph()
    spacer.paragraph_format.space_after = Pt(2)
    spacer.paragraph_format.line_spacing = 1.0
    return t


def add_figure(name, caption, width_cm=15.4):
    path = os.path.join(ROOT, "docs", "figures", name)
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.space_before = Pt(6)
    p.paragraph_format.space_after = Pt(2)
    p.paragraph_format.keep_with_next = True
    p.add_run().add_picture(path, width=Cm(width_cm))
    cap = doc.add_paragraph()
    cap.alignment = WD_ALIGN_PARAGRAPH.CENTER
    r = cap.add_run(caption)
    _set_run(r, cn="楷体", size=10)
    cap.paragraph_format.space_after = Pt(8)
    return p


# ============ 封面 ============
title("安溯·知源 VeriTrace")
subtitle("AI 驱动的农产品可信溯源决策平台 · 技术文档")
subtitle("第九届传智杯全国大学生数智创新与 AI 应用大赛 · AI WEB 网页开发挑战赛")
info = add_table(
    ["项目名称", "安溯·知源 VeriTrace（农产品全链路可信溯源决策平台）"],
    [
        ["赛道", "AI WEB 网页开发挑战赛（团队赛）"],
        ["技术栈", "Spring Boot 4 / Java 17 / MyBatis / MySQL 8 / Vue 3 / TypeScript / Vite / Element Plus / ECharts"],
        ["AI 能力", "通义千问 qwen-plus（Agent）、qwen3-vl-plus（多模态）、text-embedding-v3（RAG）"],
        ["文档版本", "V1.0"],
        ["指导教师", "满国晶"],
    ],
    widths=[3.5, 12.5],
)


def add_toc():
    """插入可刷新的 Word 目录域（Heading 1–2）。"""
    # 目录标题
    tp = doc.add_paragraph()
    tr = tp.add_run("目录")
    _set_run(tr, cn="黑体", size=16, bold=True)
    tp.alignment = WD_ALIGN_PARAGRAPH.CENTER
    tp.paragraph_format.space_before = Pt(14)
    tp.paragraph_format.space_after = Pt(10)

    p = doc.add_paragraph()
    run = p.add_run()
    fld_begin = OxmlElement("w:fldChar")
    fld_begin.set(qn("w:fldCharType"), "begin")
    instr = OxmlElement("w:instrText")
    instr.set(qn("xml:space"), "preserve")
    instr.text = 'TOC \\o "1-2" \\h \\z \\u'
    fld_sep = OxmlElement("w:fldChar")
    fld_sep.set(qn("w:fldCharType"), "separate")
    placeholder = OxmlElement("w:t")
    placeholder.text = "（打开文档后右键“更新域”即可生成目录页码）"
    fld_end = OxmlElement("w:fldChar")
    fld_end.set(qn("w:fldCharType"), "end")
    run._r.append(fld_begin)
    run._r.append(instr)
    run._r.append(fld_sep)
    run._r.append(placeholder)
    run._r.append(fld_end)
    # 目录后分页
    doc.add_page_break()


add_toc()

# ============ 1 项目概述 ============
h1("1  项目概述")

h2("1.1  项目背景")
body("农产品溯源长期停留在“信息展示”层面：消费者扫码后只能看到检测报告、农事记录和流转表格，"
     "仍然无法判断这批菜该不该买、怎么保存、出了问题由谁负责；农户的品质与承诺缺少可兑现、可追责的记录；"
     "平台与消费者之间通常是一次性扫码关系，缺少身份沉淀与长期服务。")
body("本项目面向上述缺口，建设一个 AI 驱动的可信溯源决策平台。平台不只回答“检了什么”，"
     "还向消费者给出购买决策、保存建议、承诺兑现状态与售后进度，并通过 AI 助手结合消费者身份与历史事件提供多轮问答。")

h2("1.2  项目定位")
body("安溯·知源 VeriTrace 的定位是：给已经信任平台的消费者，配一个替他判断、替他记录、替他追责的消费助手。"
     "平台不做支付、物流与店铺装修，交付的是“判断与责任”，商品交易在线下或既有渠道完成。")

h2("1.3  核心目标")
bullet("把溯源从一次性信息展示，升级为“建档—关注—提醒—复购—售后”的长期信任关系。")
bullet("用可复核的规则引擎生成可信指数，每条结论可展开查看真实证据，而非大模型主观打分。")
bullet("以 Agent 工具调用、RAG 知识检索与多模态识别构成 AI 链路，回答必须挂接真实业务数据与知识依据。")
bullet("建立农户事前承诺、系统自动兑现判定与售后 SLA 闭环，让信任可追责、可延续。")

h2("1.4  工程规模概览")
body("项目采用单仓库前后端一体结构：后端为 65 个 Java 类，前端为 47 个 Vue / TypeScript 源文件，"
     "SQL 脚本 9 份，覆盖基础库表、信任者体系、RAG 知识库与演示种子数据；AI 知识库共 126 条，其中农事标准类 102 条、消费场景类 24 条。")

# ============ 2 需求分析 ============
h1("2  需求分析")

h2("2.1  用户角色")
add_table(
    ["角色", "核心诉求", "主要功能"],
    [
        ["消费者（信任者）", "判断商品是否可信、获得保存建议、跟进售后、被记住偏好", "扫码溯源、可信指数与决策翻译、邀请建档、关注订阅、AI 助手问答、售后提交与进度查询"],
        ["农户", "展示品质与承诺、沉淀信任者与复购", "批次与农事录入、检测报告维护、信任承诺录入、个人经营看板"],
        ["经销商", "记录流转链路、触达信任者", "入库出库登记、经手批次查询、邀请码转发"],
        ["管理员", "统筹数据、处理售后、监控全局", "用户与批次管理、工单处理与超时升级、数据大屏、报表导出"],
    ],
    widths=[3.0, 5.5, 7.5],
)

h2("2.2  功能性需求")
bullet("溯源查询：按溯源码查看批次档案、农事记录、检测报告、流转链路与可信指数。")
bullet("可信评估：规则引擎五维打分，输出分级结论、证据明细与决策翻译文案。")
bullet("信任者关系：邀请码 / 扫码建档、关注农场与批次、常购订阅、上市提醒、行为事件记录。")
bullet("AI 助手：结合消费者身份、当前批次与历史事件多轮问答，展示调用工具与回答依据。")
bullet("承诺与履约：农户录入新鲜度、采收期、品质等承诺，系统按真实流转时间自动判定兑现状态。")
bullet("售后闭环：提交售后工单，记录首次响应时间与 SLA 时限，超时自动标红升级，消费者端可见进度。")

h2("2.3  非功能性需求")
add_table(
    ["类别", "要求"],
    [
        ["权限与隔离", "农户仅可操作名下批次，经销商仅可见经手批次；消费者未建档返回业务错误而非强制跳转登录"],
        ["数据一致性", "查询统一过滤软删除标记；多步写操作使用事务保证一致性"],
        ["可追溯性", "AI 回答须挂接批次事实与知识来源，数据缺失时明确说明，不以模型猜测填充"],
        ["可用性", "未配置 AI Key 时 AI 能力降级为规则文案，页面其余功能不受影响"],
        ["可部署性", "前端构建产物内嵌后端静态目录，单服务可运行；提供 Docker Compose 一键部署"],
    ],
    widths=[3.5, 12.5],
)

# ============ 3 系统架构设计 ============
h1("3  系统架构设计")

h2("3.1  总体架构")
body("系统采用前后端分层、单服务部署结构。前端为 Vue 3 单页应用，构建产物内嵌后端静态资源目录；"
     "后端为 Spring Boot 应用，按控制器、服务、实体、Mapper 分层；数据存储使用 MySQL 8；"
     "AI 能力通过外部 DashScope 模型服务接入。")
add_table(
    ["层次", "技术选型", "职责"],
    [
        ["前端表现层", "Vue 3 + TypeScript(strict) + Vite + Element Plus + ECharts + Vue Router + Axios", "页面渲染、交互、图表、会话状态与路由封装"],
        ["后端接口层", "Spring Boot 4 / Java 17 / Spring MVC", "REST 控制器、SSE 流式推送、令牌鉴权（X-Auth-Token）与会话兜底、参数校验"],
        ["业务服务层", "Java 服务类（Agent、评分、承诺、知识、消费者、安全过滤等）", "规则计算、AI 编排、业务规则与事务控制"],
        ["数据访问层", "MyBatis 注解式 Mapper", "实体与数据库之间的映射与 SQL 执行"],
        ["数据存储", "MySQL 8（utf8mb4）", "业务数据、信任者体系、承诺履约与 RAG 知识库"],
        ["外部 AI 服务", "qwen-plus / qwen3-vl-plus / text-embedding-v3", "对话 Agent、图片识别、向量检索"],
    ],
    widths=[3.0, 6.5, 6.5],
)

add_figure("fig1_arch.png", "图 3-1  系统总体架构：前端单页应用、后端分层与外部 AI 服务")

h2("3.2  后端目录结构")
add_table(
    ["包路径", "说明"],
    [
        ["controller / controller.api", "页面控制器与各角色 REST 接口（公开溯源、消费者、农户、经销商、管理员、AI）"],
        ["service", "AgentService、TrustScoreService、TrustPromiseService、KnowledgeService、ConsumerService、AiSafetyService 等核心业务服务"],
        ["entity / mapper", "数据实体与注解式 Mapper"],
        ["config", "Web 配置、登录拦截器、令牌解析过滤器（TokenAuthFilter）、密码加密、售后 SLA 定时扫描任务"],
        ["common", "统一响应结构、业务异常与全局异常处理、密码工具"],
        ["util", "二维码生成、局域网 IP 探测、Excel 导出工具"],
    ],
    widths=[5.0, 11.0],
)

h2("3.3  数据模型")
body("在基础业务表（批次、农事记录、流转记录、检测报告、投诉、扫码日志、系统用户）之上，"
     "围绕信任者体系新增六类核心表：")
add_table(
    ["数据表", "作用"],
    [
        ["consumer_account", "信任者账户：昵称、手机号、信任等级、邀请来源、家庭画像"],
        ["consumer_follow", "关注关系：关注农户 / 批次 / 作物，作为上市提醒与首页动态来源"],
        ["consumer_subscription", "常购订阅：作物、批次、数量、复购周期与下次提醒日期"],
        ["consumer_event", "行为事件流：扫码、提问、关注、订阅、售后、复购等事件记录"],
        ["trust_promise", "信任承诺：承诺类型、承诺值、证据与状态，关联具体批次"],
        ["fulfillment_record", "履约记录：发货 / 到达时间、质量标记、售后响应耗时"],
    ],
    widths=[5.0, 11.0],
)

h2("3.4  关键接口")
body("消费者侧接口挂在 `/api/consumer` 下，AI 接口挂在 `/api/ai` 下，统一返回 `code / msg / data` 结构。")
add_table(
    ["接口", "方法", "说明"],
    [
        ["/api/consumer/register", "POST", "邀请码 / 扫码建档，自动关注邀请人"],
        ["/api/consumer/me", "GET", "查询画像、信任等级、关注数与待办数"],
        ["/api/consumer/home", "GET", "首页聚合：关注清单、生长中批次、待办、助手提示"],
        ["/api/consumer/follow", "POST / DELETE", "关注 / 取消关注目标"],
        ["/api/consumer/subscription", "POST / GET", "新增 / 查询常购订阅"],
        ["/api/consumer/event", "POST", "写入消费者行为事件"],
        ["/api/ai/chat", "POST", "AI 助手单轮问答（含工具调用与依据）"],
        ["/api/ai/chat/stream", "POST", "SSE 流式问答，推送工具进度与逐字回答"],
        ["/api/ai/health-report", "POST", "生成批次 AI 体检报告"],
    ],
    widths=[5.5, 2.5, 8.0],
)

# ============ 4 AI 技术方案 ============
h1("4  AI 技术方案")

h2("4.1  选型理由")
body("赛道要求综合运用大语言模型、AI Agent、RAG 检索增强与多模态 AI。本项目在选型上遵循“事实层与话术层分离”的原则："
     "涉及分数、结论与责任判断的部分由确定性规则与数据库事实承担，大模型只负责把事实翻译成消费者可理解的表达，"
     "从而保证结论可复核、可追责，避免模型主观判断带来的不可追溯风险。")
add_table(
    ["能力", "所选模型 / 方案", "选型理由"],
    [
        ["对话与工具调用", "qwen-plus", "支持 function calling，可在多轮对话中按意图调用业务工具并回喂结果"],
        ["多模态识别", "qwen3-vl-plus", "识别消费者上传的菜品 / 包装 / 检测照片，识别结果再进入 Agent 链路"],
        ["知识向量化", "text-embedding-v3", "将标准与消费场景知识转向量，支持语义相似度检索"],
        ["可信评分", "自研规则引擎", "分数对应真实证据，可逐项复核，不交由大模型主观打分"],
    ],
    widths=[3.0, 4.0, 9.0],
)

h2("4.2  Agent 工具调用机制")
body("AgentService 基于通义千问 function calling 实现多轮工具编排。流程为：组装系统提示与消费者、批次、历史上下文，"
     "提交对话模型；模型返回工具调用后，服务端在限定当前批次的范围内执行工具，将结果回喂模型，"
     "循环至模型给出最终回答；前端通过 SSE 接收工具进度、逐字回答与调用工具徽章。")
body("当前工具集覆盖批次信息、农事记录、检测报告、流转链路、知识库检索，以及常购清单、历史事件、当季推荐等信任者工具；"
     "所有工具调用均限定在当前批次或当前消费者身份范围内，防止越权读取。")
body("Agent 最多进行 5 轮工具编排，每个会话在内存中保留最近 20 条消息作为上下文；工具返回的事实、调用过的工具名称与答案依据，"
     "随回答一并返回前端，分别用于工具徽章展示与来源核验。")
add_table(
    ["工具名称", "标签", "作用范围"],
    [
        ["get_batch_info", "批次信息", "查询当前批次作物、品种、产地、播种/采收日期与状态"],
        ["get_farming_logs", "农事记录", "查询打药、施肥、灌溉、采收等农事记录及日期物料用量"],
        ["get_detection_reports", "检测报告", "查询第三方农残检测报告编号、机构、日期与结论"],
        ["get_trace_records", "流转链路", "查询入库、出库、运输等流转环节与地点"],
        ["search_knowledge", "知识库检索", "检索农残限量、安全间隔期、储存保鲜、清洗技巧等通用知识"],
        ["get_my_basket", "我的常购", "查询当前消费者常购清单与到期日（需登录）"],
        ["get_my_history", "我的历史", "查询最近消费、提问与售后历史（需登录）"],
        ["recommend_in_season", "当季推荐", "返回关注农场中当季可预约的批次（需登录）"],
    ],
    widths=[4.2, 2.6, 9.2],
)

add_figure("fig2_agent.png", "图 4-1  Agent 五轮工具调用循环：预取、回喂、轮次判定与流式输出")

h2("4.3  RAG 检索增强")
body("KnowledgeService 承担农事标准与消费场景知识的检索。知识入库时调用 embedding 接口生成向量并回写数据库，"
     "检索时对问题向量化后与库内向量计算余弦相似度，取相关度达标的 top-k 条目；"
     "向量结果在内存中缓存以降低重复计算，并在知识库变化后自动重建。当向量不可用或相似度不足时，"
     "降级为关键词匹配检索，保证链路可用。")
body("知识库包含标准类与消费场景类条目，覆盖储存、食用、选购等场景；检索结果作为回答依据之一，随回答一并提供给前端核验。")

add_figure("fig3_rag.png", "图 4-2  RAG 检索链路：向量召回、阈值判定、关键词降级与依据注入")

h2("4.4  多模态融合路径")
body("消费者上传图片后，先由视觉模型识别画面内容，识别结果拼入文本问题再交给 Agent 处理，"
     "不直接把视觉模型结论作为最终答复。该设计让图片识别与业务数据、知识检索在同一 Agent 链路内融合。")

h2("4.5  安全与可靠性策略")
bullet("输入拦截：单次问题限制在 1000 字以内，识别提示注入、角色劫持与违规请求特征，命中时返回拒绝话术。")
bullet("证据回喂：服务端先预取批次、农事、检测、流转与知识库数据，模型基于预取事实作答。")
bullet("出口兜底：未命中知识或数据缺失时，明确提示以包装标签或检测报告原文为准，不臆造结论；对疑似违规输出与系统提示词泄露做二次拦截。")
bullet("降级可用：未配置 API Key 时，AI 问答自动降级为根据批次数据生成的规则文案，不影响其他功能演示。")

h2("4.6  优化策略")
bullet("上下文分层注入：消费者身份、当前批次、历史事件分段组织提示，避免无关信息干扰工具选择。")
bullet("工具范围收敛：工具参数与数据访问限定批次和身份，减少越权与误调用。")
bullet("向量缓存与降级：内存缓存向量并提供关键词兜底，兼顾检索效率与可用性。")
bullet("回答结构化：结论在前、依据在后、下一步动作收尾，便于消费者核验与继续操作。")

# ============ 5 核心功能实现 ============
h1("5  核心功能实现")

h2("5.1  可信指数规则引擎")
body("TrustScoreService 按五个维度计算百分制可信指数，并输出红黄绿分级与决策结论。各维度权重与依据如下：")
add_table(
    ["维度", "权重", "评分依据"],
    [
        ["第三方检测报告", "30 分", "检测报告齐全度与结论"],
        ["农事记录完整度", "20 分", "播种、施肥、打药、灌溉等关键记录各占 5 分"],
        ["安全间隔期合规", "20 分", "最后一次打药距采收 / 当前时间是否达到间隔要求"],
        ["流转链路完整性", "20 分", "采收、入库、出库 / 销售节点完整度"],
        ["数据新鲜度", "10 分", "最近一次记录更新时间"],
    ],
    widths=[4.0, 2.5, 9.5],
)
body("每个维度都附带可展开的证据明细；评分之上由决策翻译层给出“建议购买 / 注意核对 / 建议谨慎”的结论，"
     "配放心理由、风险提示与保存建议。")

add_figure("fig4_score.png", "图 5-1  可信指数五维权重、加权汇总与红黄绿分级")

h2("5.2  决策翻译")
body("决策翻译把规则分数与业务数据转换为消费者可直接理解的结论与建议，例如结合检测齐全度与间隔期情况，"
     "说明是否可放心食用、需要注意哪些事项。翻译层只复用事实数据，不改变证据本身。")

h2("5.3  信任承诺与兑现判定")
body("TrustPromiseService 管理批次承诺的录入与兑现判定。承诺类型涵盖新鲜时效、采收期、品质与售后响应等；"
     "系统结合流转记录与履约记录中的实际时间，逐条判定已兑现或待兑现，并计算兑现得分。"
     "承诺记录基于真实时间判定，不可事后篡改。")

h2("5.4  信任者长期上下文")
body("ConsumerService 管理账户、关注、订阅、首页聚合与事件写入。助手在问答时注入消费者画像、关注关系与历史事件，"
     "支持“我常买什么”“上次那个批次怎么样”等结合个人历史的追问；行为事件持续沉淀，供助手与农户侧看板使用。")

h2("5.5  售后 SLA 闭环")
body("售后工单记录承诺响应时限与首次响应时间；定时任务扫描临近或超时工单，超时后工单标红并进入管理员红色角标。"
     "消费者端实时查看处理进度与倒计时，形成提交、受理、处理、办结、确认的闭环。")

h2("5.6  权限隔离与工程约定")
bullet("身份与数据边界：农户只能操作名下批次，经销商只能查看经手批次；消费者接口未建档返回业务错误。")
bullet("软删除：业务查询统一过滤删除标记，避免已删除数据进入展示。")
bullet("事务控制：多步写操作加入事务，保证批次、记录与事件写入一致。")
bullet("统一响应：接口统一 `code / msg / data` 结构，全局异常处理转为可读错误信息。")

h2("5.7  前端工程化实践")
body("前端为 Vue 3 + TypeScript（strict）+ Vite 单页应用，配合 Element Plus 组件库与 ECharts 图表；构建产物内嵌后端 "
     "`static/app/` 目录，与后端同源部署，省去跨域配置与二次鉴权，单服务即可完整运行。")
bullet("路由分层：`createWebHistory('/app/')` 挂载于后端页面前缀下；溯源详情、邀请建档、信任者首页、消费助手、投诉提交与查询等路由标记为公开路由，扫码即用不需登录；其余路由在全局守卫中先拉取身份，未登录跳转登录页并携带原路径。")
bullet("角色隔离：守卫比对路由声明的角色与当前身份，不一致时给出提示并跳回该身份的工作台，避免多标签页切换账号后出现越权页面或“页面自己乱跳”的困惑。")
bullet("令牌存储：登录令牌写入请求头 `X-Auth-Token`，以 sessionStorage 为主、localStorage 兜底——同一浏览器多标签页可分别登录不同角色，新开标签页又不必重复登录。")
bullet("统一响应解包：axios 响应拦截器把 `{code, msg, data}` 解包为业务数据，403 统一走权限提示，文件流（二维码、Excel 导出）以 blob 直接透传。")
bullet("流式渲染：消费助手用流式接口逐段渲染工具步骤与回答正文，工具徽章与依据来源随内容同步出现，避免长时间白屏等待。")
bullet("布局复用：三套角色端共用同一套控制台布局组件，顶栏、抽屉菜单与角色菜单表集中维护，新增菜单只改一处。")

# ============ 6 测试与部署 ============
h1("6  测试与部署")

h2("6.1  测试说明")
body("项目包含基础测试依赖（Spring MVC 测试、MyBatis 测试）。演示阶段以固定数据集与演示账号进行功能走查，"
     "覆盖扫码溯源、可信指数、助手问答、承诺录入、关注订阅与售后工单等主流程；"
     "AI 相关链路在配置有效 API Key 后进行真实调用验证，未配置时验证规则降级文案输出。")
add_table(
    ["测试模块", "走查内容", "预期结果"],
    [
        ["溯源查询", "按溯源码打开批次档案，查看农事、检测、流转与可信指数", "档案与证据明细完整展示，指数按规则分级"],
        ["可信指数与决策翻译", "展开五维证据，查看结论分级、理由与保存建议", "分数与证据一一对应，结论与建议一致"],
        ["AI 助手", "提问常购、历史批次与选购保存类问题，查看工具徽章与依据", "返回基于事实的回答，展示调用工具与来源，缺失信息明示"],
        ["承诺与履约", "农户录入承诺，消费者侧查看兑现状态与兑现得分", "按真实时间判定已兑现/待兑现，记录不可篡改"],
        ["关注订阅", "建档、关注农场与批次、设置常购订阅与提醒", "首页聚合关注清单、生长中批次与待办"],
        ["售后工单", "提交投诉、查看 SLA 倒计时、管理员端处理与超时升级", "超时标红并进入管理员角标，消费者端进度可见"],
        ["权限隔离", "农户、经销商、消费者分别访问受限数据", "越权数据不可见，未建档消费者收到业务错误而非强制登录"],
        ["降级链路", "不配置 AI_API_KEY 触发降级", "AI 问答输出规则文案，其余功能正常可用"],
    ],
    widths=[3.0, 6.5, 6.5],
)

h2("6.2  本地部署")
body("本地环境要求：JDK 17、Maven 3.9+、Node 20 LTS、MySQL 8.0。步骤如下：")
bullet("创建数据库 nongyeplus（utf8mb4），按顺序导入 sql 目录下基础库、信任者体系、知识库与种子数据 SQL。")
bullet("进入 frontend 目录执行 npm install、npm run type-check、npm run build，产物输出到后端静态资源目录。")
bullet("配置环境变量（可选 AI_API_KEY），执行 mvn spring-boot:run 启动后端。")
body("详细变量、依赖与排查项见仓库《环境配置与部署说明》《依赖清单》及 `.env.example` 模板。")

h2("6.3  容器化部署")
body("项目提供 Dockerfile 与 docker-compose.yml，采用多阶段构建：第一阶段用 Node 20 构建前端，"
     "第二阶段用 Maven 构建后端 Jar 并合入前端产物，第三阶段使用精简 JRE 运行镜像。"
     "MySQL 容器首次启动自动初始化基础库与知识库，应用通过依赖健康检查启动；上传文件使用数据卷持久化。")

add_figure("fig5_deploy.png", "图 6-1  单机部署拓扑：Compose 容器编排与外部 AI 服务调用")

h2("6.4  环境变量与外部依赖")
body("项目不将密钥写入代码，运行参数通过环境变量注入，模板见仓库 `.env.example`；外部服务与中间件依赖如下：")
add_table(
    ["配置项 / 依赖", "说明"],
    [
        ["DB_USERNAME / DB_PASSWORD", "MySQL 连接账号与密码，默认 root / 123456，生产环境须修改"],
        ["AI_API_KEY", "DashScope 通义千问访问密钥；留空时 AI 能力降级为规则文案"],
        ["AI_AGENT_MODEL / AI_VL_MODEL / AI_EMBEDDING_MODEL", "对话、视觉、向量模型名称，默认 qwen-plus / qwen3-vl-plus / text-embedding-v3"],
        ["APP_BASE_URL", "对外访问基础地址，用于溯源链接与二维码生成"],
        ["MySQL 8.0", "业务数据、信任者体系、承诺履约与 RAG 知识库持久化"],
        ["DashScope 模型服务", "对话 Agent、图片识别与向量检索，经 HTTPS 接口调用"],
    ],
    widths=[5.5, 10.5],
)

h2("6.5  演示账号与入口")
add_table(
    ["角色", "入口", "账号", "密码"],
    [
        ["管理员", "/app/login", "admin", "123456"],
        ["农户", "/app/login", "farmer01", "123456"],
        ["经销商", "/app/login", "dealer01", "123456"],
        ["消费者", "/app/trace/TZ202603001001", "无需登录", "—"],
    ],
    widths=[3.0, 5.5, 3.5, 4.0],
)

h2("6.6  性能与工程约束")
bullet("减少模型轮次：服务端先预取批次、农事、检测、流转与知识数据，模型多数问题一轮即可作答，显著减少工具调用往返。")
bullet("缓存复用：知识向量在内存中缓存，仅在知识库条目变更时重建，避免每次提问都重复计算向量。")
bullet("检索降级：向量服务不可用或相似度不足时改走关键词匹配，保证问答链路在外部依赖异常时仍可返回。")
bullet("单服务部署：前端产物内嵌后端，静态资源带内容哈希便于缓存失效；浏览器与接口同源，减少一次跨域预检开销。")
bullet("数据访问：统一软删除过滤与事务边界，导出、二维码等重操作以文件流返回，不占用页面渲染线程。")
body("上述措施均为工程层面的可用性设计，文档不宣称具体性能指标；实际并发能力取决于部署机器的 MySQL 与应用配置，"
     "演示环境按单机单用户流量设计。")

# ============ 7 创新点 ============
h1("7  创新点说明")

h2("7.1  技术创新点")
bullet("规则与模型分离的可信评估：可信指数由确定性规则引擎按真实证据打分，大模型只做翻译，结论可逐项复核、可追责。")
bullet("身份感知的 Agent 助手：Agent 注入消费者身份、关注关系与历史事件，工具调用限定批次与身份范围，回答可追溯到业务事实。")
bullet("可降级的 RAG 链路：向量检索配合余弦相似度与关键词降级，知识依据随回答展示，兼顾语义检索与链路可用性。")
bullet("承诺自动兑现判定：基于真实流转与履约时间自动判定承诺状态，把口头品质承诺转化为可记录的履约数据。")

h2("7.2  应用创新点")
bullet("从“信息展示”到“消费决策”：面向消费者输出购买判断、保存建议与风险提示，而非罗列数据表格。")
bullet("从“一次性扫码”到“长期关系”：通过建档、关注、订阅与事件沉淀，形成复购与提醒的长期服务闭环。")
bullet("从“投诉无下文”到“责任闭环”：SLA 倒计时、超时升级与进度可见，让售后成为可跟踪的责任通道。")
bullet("边界清晰的价值定位：不介入交易履约，聚焦判断与责任，区别于普通电商与常规溯源平台。")

# ============ 8 总结与展望 ============
h1("8  总结与展望")

h2("8.1  工作总结")
body("本项目围绕农产品信任场景，完成了覆盖农户、经销商、管理员与消费者四类角色的溯源与服务闭环，"
     "实现可信指数评估、决策翻译、信任承诺兑现、信任者关系管理、AI 助手问答与售后 SLA 闭环等功能，"
     "并落地 Agent 工具调用、RAG 检索与多模态识别相结合的 AI 链路。")

h2("8.2  当前边界")
bullet("不做支付、物流、店铺装修与优惠券体系，交易在线下或既有渠道完成。")
bullet("RAG 知识库为演示种子数据，评分维度与权重尚需结合真实农户与消费者反馈进一步校准。")
bullet("当前检索在小样本下采用全表扫描，后续数据规模扩大后可引入专用向量索引。")

h2("8.3  后续规划")
bullet("扩充检测标准与消费场景知识，完善承诺兑现率、复购率等经营指标看板。")
bullet("接入小程序 / 公众号开放能力，完善邀请码体系与行为事件回流链路。")
bullet("基于真实使用反馈校准评分维度，沉淀可复核的 AI 回答评测集。")
bullet("在保证可追溯的前提下，扩展多批次对比、季节推荐与家庭画像等个性化能力。")

doc.save(OUT)
print("saved:", OUT)
print("paragraphs:", len(doc.paragraphs), "tables:", len(doc.tables))
