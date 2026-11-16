# -*- coding: utf-8 -*-
"""生成《安溯·知源 VeriTrace 作品介绍文档》DOCX。

严格对应传智杯提交表单第 6 项"作品介绍文档"的 7 条具体要求：
(1) 描述项目详情及使用方式   (2) 描述项目的亮点   (3) AI 测试技术落地使用情况概述
(4) 描述项目中涉及的关键技术 (5) 项目相关的图片/照片 (6) 之前是否发表过或获奖
(7) 若项目是在其他项目基础上优化，请说明优化部分
"""
import os

from docx import Document
from docx.shared import Pt, Cm, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT
from docx.enum.section import WD_SECTION
from docx.oxml.ns import qn
from docx.oxml import OxmlElement

ROOT = r"D:\若依\nongye"
SHOT = os.path.join(ROOT, "deploy", "screenshots")
OUT = os.path.join(ROOT, "docs", "安溯知源-作品介绍文档.docx")

doc = Document()

sec = doc.sections[0]
sec.page_width = Cm(21.0)
sec.page_height = Cm(29.7)
sec.top_margin = Cm(2.5)
sec.bottom_margin = Cm(2.5)
sec.left_margin = Cm(2.5)
sec.right_margin = Cm(2.5)

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


def subtitle(text, size=12, cn="楷体"):
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    r = p.add_run(text)
    _set_run(r, cn=cn, size=size)
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


def numbered(text):
    """有序列表：自动编号由 Word 列表样式维护。"""
    p = doc.add_paragraph(style="List Number")
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
            p.alignment = WD_ALIGN_PARAGRAPH.CENTER
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


def table_caption(text):
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    r = p.add_run(text)
    _set_run(r, cn="楷体", size=10.5)
    p.paragraph_format.space_before = Pt(6)
    p.paragraph_format.space_after = Pt(2)
    p.paragraph_format.keep_with_next = True
    return p


def add_figure(filename, caption, width_cm=9.0):
    path = os.path.join(SHOT, filename)
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.space_before = Pt(6)
    p.paragraph_format.space_after = Pt(2)
    p.paragraph_format.keep_with_next = True
    p.add_run().add_picture(path, width=Cm(width_cm))
    cap = doc.add_paragraph()
    cap.alignment = WD_ALIGN_PARAGRAPH.CENTER
    r = cap.add_run(caption)
    _set_run(r, cn="楷体", size=10.5)
    cap.paragraph_format.space_after = Pt(8)
    return p


# ===================== 封面 =====================
title("安溯·知源 VeriTrace")
subtitle("AI 驱动的农产品可信溯源决策平台")
subtitle("作品介绍文档", size=14, cn="黑体")

add_table(
    ["项目信息", "内容"],
    [
        ["作品名称", "安溯·知源 VeriTrace（农产品全链路可信溯源决策平台）"],
        ["参赛赛道", "AI WEB 网页开发挑战赛（团队赛）"],
        ["参赛组别", "B组"],
        ["参赛学校", "哈尔滨信息工程学院"],
        ["团队名称", "熬夜敢死队plus"],
        ["团队成员", "程俊、华天瑞"],
        ["指导教师", "满国晶"],
        ["线上演示", "http://47.243.26.118/（无需安装，浏览器直接打开）"],
        ["文档日期", "2026 年 11 月"],
    ],
    widths=[3.5, 12.5],
)

doc.add_page_break()

# ===================== 目录 =====================
tp = doc.add_paragraph()
tr = tp.add_run("目　录")
_set_run(tr, cn="黑体", size=16, bold=True)
tp.alignment = WD_ALIGN_PARAGRAPH.CENTER
tp.paragraph_format.space_before = Pt(0)
tp.paragraph_format.space_after = Pt(12)

toc_p = doc.add_paragraph()
toc_run = toc_p.add_run()
fld_begin = OxmlElement("w:fldChar")
fld_begin.set(qn("w:fldCharType"), "begin")
instr = OxmlElement("w:instrText")
instr.set(qn("xml:space"), "preserve")
instr.text = 'TOC \\o "1-2" \\h \\z \\u'
fld_sep = OxmlElement("w:fldChar")
fld_sep.set(qn("w:fldCharType"), "separate")
placeholder = OxmlElement("w:t")
placeholder.text = "（目录页码将在打开文档时自动更新；如未更新，请全选后按 F9）"
fld_end = OxmlElement("w:fldChar")
fld_end.set(qn("w:fldCharType"), "end")
toc_run._r.append(fld_begin)
toc_run._r.append(instr)
toc_run._r.append(fld_sep)
toc_run._r.append(placeholder)
toc_run._r.append(fld_end)

# 正文另起一页，并从第 1 页编号
body_sec = doc.add_section(WD_SECTION.NEW_PAGE)
body_sec.footer.is_linked_to_previous = False
pg_num = OxmlElement("w:pgNumType")
pg_num.set(qn("w:start"), "1")
body_sec._sectPr.append(pg_num)

fp = body_sec.footer.paragraphs[0]
fp.alignment = WD_ALIGN_PARAGRAPH.CENTER
frun = fp.add_run()
_pb = OxmlElement("w:fldChar")
_pb.set(qn("w:fldCharType"), "begin")
_pi = OxmlElement("w:instrText")
_pi.set(qn("xml:space"), "preserve")
_pi.text = " PAGE "
_ps = OxmlElement("w:fldChar")
_ps.set(qn("w:fldCharType"), "separate")
_pe = OxmlElement("w:fldChar")
_pe.set(qn("w:fldCharType"), "end")
frun._r.append(_pb)
frun._r.append(_pi)
frun._r.append(_ps)
frun._r.append(_pe)
_set_run(frun, size=10.5)

# ===================== 一、项目详情及使用方式 =====================
h1("一、项目详情及使用方式")

h2("（一）项目定位")
body("安溯·知源 VeriTrace 是一个面向农产品全链路的可信溯源与消费决策平台，覆盖种植建档、批次流转、"
     "检测留档、扫码溯源、消费决策和售后追踪六个环节，供农户、经销商、平台管理员和消费者四类角色使用。")
body("项目要回答的问题，不是“这批农产品来自哪里”，而是“这批农产品值不值得信任、现在该不该买”。"
     "市面上多数溯源系统停在信息展示：消费者扫码看到一份检测报告和几条流转记录，看完即结束，既看不懂，也无从追问。"
     "本项目把消费者端从“扫一次码、看一次报告”的一次性查询，改造成“建档—关注—提醒—复购—售后”的长期关系，"
     "并让 AI 在每一个环节承担决策翻译，而不是只做信息搬运。")

h2("（二）要解决的四个具体问题")
numbered("检测报告看不懂。农残数据、流转节点属于专业信息，消费者真正关心的是能不能放心买。")
numbered("承诺无人核对。农户和经销商对外说“现摘现发”，说完没有下文，没有任何机制验证是否兑现。")
numbered("投诉没有闭环。消费者反馈缺少处理时限与进度，问题容易石沉大海。")
numbered("关系随扫码结束。扫码行为是匿名的，消费者下次再来，平台既不认识他，也不记得他问过什么。")

h2("（三）系统组成")
body("系统分为四端。农户端负责种植批次建档、农事记录与检测报告上传；经销商端负责入库、出库与承诺发布；"
     "管理端提供数据大屏、投诉工单与知识库维护；消费者端包括溯源查询页、AI 消费助手和“我的信任”中心。"
     "四端共用一套鉴权与数据权限，消费者端免登录即可扫码访问。")

h2("（四）使用方式")
body("作品已完成公网部署，无需安装任何本地环境，用浏览器打开 http://47.243.26.118/ 即可直接体验。"
     "管理端、农户端、经销商端的演示账号如下表，消费者端不登录也能使用。")

table_caption("表 1　线上演示入口与演示账号")
add_table(
    ["入口", "地址或账号", "说明"],
    [
        ["统一登录", "http://47.243.26.118/app/login", "三端共用登录页"],
        ["管理员", "admin / 123456", "数据大屏、投诉工单、知识库"],
        ["农户", "farmer01 / 123456", "批次建档、农事记录"],
        ["经销商", "dealer01 / 123456", "入库出库、承诺发布"],
        ["消费者溯源", "http://47.243.26.118/trace/TZ202603001001", "免登录，扫码即可查看"],
        ["AI 消费助手", "http://47.243.26.118/app/assistant/TZ202603001001", "基于该批次上下文对话"],
    ],
    widths=[3.2, 9.8, 4.0],
)

h2("（五）典型操作路径")
body("以消费者视角为例：打开溯源链接后，先看到该批次的可信指数与五个维度的评分，每一条评分都可以点开查看对应证据；"
     "接着在 AI 助手中追问“这个菜怎么存放”“现在买合适吗”，助手会结合批次事实与知识库给出结论；"
     "消费者可以建立档案、关注该批次或该农户并订阅上新提醒；若遇到质量问题，直接发起投诉，平台按约定时限受理并更新进度。"
     "管理端则可以在数据大屏上看到批次、农户、经销商、待处理投诉和扫码量等指标，并对投诉工单实时处理。")

# ===================== 二、项目亮点 =====================
h1("二、项目亮点")

h2("（一）可信指数：把“可信”算成可解释的分数")
body("项目没有把“可信”停留在口号上，而是拆成五个维度、合计一百分的指标体系，由规则引擎计算。"
     "每个维度都对应可核查的原始数据，分数下方可以逐条展开证据，包括检测报告、流转记录、农事记录和农户资质。"
     "用户看到的不是一句“品质优良”，而是一个能追问到源头的分数。")

h2("（二）决策翻译：AI 回答“该不该买”，而不是“有什么”")
body("常规问答式助手只会复述数据库字段。本项目的 AI 消费助手在回答前会先判断需要哪些事实，"
     "再通过工具调用取回批次、检测、流转、承诺和知识库内容，最后用消费者听得懂的话给出判断与依据。"
     "线上实测中，询问储存方法时，模型自主发起知识库检索（search_knowledge），并把检索到的条目标注为回答出处。")

h2("（三）承诺兑现：把口头保证变成可核验的事实")
body("农户和经销商可以对外发布承诺，例如“采后 24 小时内入库”。平台记录承诺内容与承诺时间，"
     "并用采收时间与入库时间计算实际耗时，自动判定是否兑现，再把结果公开展示在溯源页上。"
     "承诺从此不是宣传语，而是一条会被核对、会被追责的记录。")

h2("（四）投诉 SLA：处理有时限，超时有标记")
body("每条投诉都带受理时限、首次响应时间与超时标记。管理端可以按待处理、处理中、已关闭筛选工单，"
     "超过时限仍未响应的投诉会被标红并计入统计，避免消费者反馈被无限期搁置。")

h2("（五）信任者长期上下文：AI 记得你上次问过什么")
body("项目为消费者建立了身份档案，记录关注、订阅与行为事件。消费者再次进入助手时，"
     "AI 能结合此前的关注批次和提问历史作答，而不是每次都从零开始。这一步把一次性的扫码流量，转成了可以持续服务的信任关系。")

table_caption("表 2　与传统溯源平台的能力对比")
add_table(
    ["能力项", "传统溯源平台", "安溯·知源 VeriTrace"],
    [
        ["溯源结果", "静态报告，看完即止", "可信指数 + 可展开的证据链"],
        ["消费者提问", "无，或固定问答", "AI 智能体按需调用工具作答"],
        ["承诺管理", "没有承诺概念", "承诺发布、耗时核算、兑现公示"],
        ["投诉处理", "留言后无进度", "时限、首响、超时标记的闭环"],
        ["消费者身份", "匿名扫码，无沉淀", "建档、关注、订阅、上下文记忆"],
    ],
    widths=[3.4, 6.3, 7.3],
)

# ===================== 三、AI 技术落地 =====================
h1("三、AI 技术落地使用情况概述")

h2("（一）接入方式与模型选型")
body("AI 能力接入阿里云百炼平台（DashScope 兼容模式），线上环境已配置真实密钥并完成调用验证。"
     "对话与智能体使用 qwen-plus，图片理解使用 qwen3-vl-plus，知识库检索使用 text-embedding-v3 做文本向量化。"
     "三类模型各司其职，不把多模态识别的成本压在对话链路上。")

h2("（二）智能体与工具调用")
body("助手不是一个把问题转给大模型的接口，而是一个带工具的智能体。系统向模型注册了批次查询、检测报告查询、"
     "流转记录查询、承诺状态查询、知识库检索等工具，由模型根据用户问题自行决定调用哪一个、按什么顺序调用。"
     "回答中的每一个关键结论都应能追溯到某次工具返回，这也是页面会展示工具调用记录的原因。")

h2("（三）RAG 知识库")
body("平台内置消费科普知识库，线上已向量化入库 126 条条目，覆盖储存方式、品质判断、食用禁忌等高频问题。"
     "检索采用向量相似度为主、关键词匹配为兜底的策略：向量服务不可用时自动降级，保证问答不中断。"
     "回答中会标注所引用的知识条目，避免模型凭空生成。")

h2("（四）多模态识别")
body("消费者可以上传农产品外观或包装标签照片，由 qwen3-vl-plus 识别图中信息并给出判断依据，"
     "识别结果作为对话上下文的一部分参与后续推理。")

h2("（五）可靠性与成本控制")
body("为避免演示环境因密钥或网络异常而白屏，系统设置了完整的降级链路：密钥缺失或调用失败时，"
     "助手退回规则化文案，页面仍可正常使用，并在日志中记录失败原因；AI 接口单独放宽超时限制至 180 秒，"
     "前端配加载态与重试入口。知识库向量化在启动时完成，避免每次问答重复计算。")

h2("（六）线上实测记录")
body("在公网环境中完成端到端验证：登录管理端后，数据大屏各指标正常渲染；进入某批次 AI 助手，"
     "提问后获得带知识库出处的回答，回答下方可见工具调用记录。这说明函数调用与检索增强并非界面模拟，"
     "而是真实走通了“模型决策—工具执行—结果回填—成文输出”的完整链路。")

# ===================== 四、关键技术 =====================
h1("四、项目中涉及的关键技术")

h2("（一）总体架构")
body("系统按接入层、应用层、数据层、智能层四层组织。接入层由 Nginx 承担静态资源分发与反向代理；"
     "应用层为单体 Spring Boot 服务，负责鉴权、业务编排与接口输出；数据层使用 MySQL 8.0 存储业务数据；"
     "智能层对接百炼平台完成对话、检索与多模态识别。前端构建产物内嵌在应用包内，整套系统以单个 Jar 交付，"
     "部署时只需一个进程加一个数据库。")

table_caption("表 3　关键技术栈与用途")
add_table(
    ["层次", "技术选型", "承担的职责"],
    [
        ["后端框架", "Spring Boot 4.0.8 / Java 17", "依赖注入、事务、统一异常与响应封装"],
        ["持久层", "MyBatis / MySQL 8.0", "14 张业务表的映射、索引与升级脚本"],
        ["前端框架", "Vue 3 / TypeScript / Vite", "组件化页面、类型约束、构建与分包"],
        ["UI 与图表", "Element Plus / ECharts", "表单、表格组件与数据大屏可视化"],
        ["鉴权", "令牌鉴权 + 角色权限隔离", "四端身份区分与数据可见范围控制"],
        ["AI 能力", "百炼 qwen-plus / qwen3-vl-plus / text-embedding-v3", "智能体、多模态识别、知识库检索"],
        ["部署运维", "阿里云 ECS / Nginx / systemd", "公网访问、反代、压缩缓存、开机自启"],
    ],
    widths=[3.0, 6.4, 7.6],
)

h2("（二）后端实现要点")
body("后端以控制器、服务、映射三层划分职责，跨表一致性操作统一走事务；对可能重复提交的接口做了幂等处理，"
     "避免演示时重复写库。全局异常处理把数据库与外设错误收敛为可读提示，接口统一返回结构便于前端判断。"
     "鉴权层区分农户、经销商、管理员与匿名消费者四类身份，数据权限按身份收窄，避免越权查看他人批次。")

h2("（三）前端与工程化")
body("前端使用 Vue 3 组合式 API 与 TypeScript，路由按角色分区，组件按页面拆分。构建通过 Vite 完成，"
     "对第三方库单独分包，产物直接输出到后端静态资源目录并随 Jar 打包，避免前后端分离部署带来的跨域与运维成本。"
     "数据大屏使用 ECharts 渲染，指标与图表共用同一批接口，减少重复请求。")

h2("（四）数据设计")
body("数据库共 14 张业务表，按角色档案、批次流转、信任关系三条主线组织。批次表承载种植与在售信息，"
     "流转表记录操作类型、数量与操作人，信任相关表分别承载消费者档案、关注、订阅、行为事件与承诺记录。"
     "结构变更通过幂等的升级脚本执行，脚本先查系统表再决定是否变更，重复执行不会报错。")

h2("（五）部署与性能优化")
body("系统部署在阿里云 ECS 云主机（中国香港地域，无需备案），Nginx 监听 80 端口反向代理到应用 8081 端口，"
     "由 systemd 托管并配置异常重启，环境变量与密钥单独存放于受限文件中。上线后针对跨境访问做了两项优化："
     "开启反向代理响应压缩，并对带哈希指纹的静态资源设置长缓存，使体积约 1 兆的核心组件由 1,089,555 字节降至 341,220 字节，"
     "首次加载由约 14.6 秒缩短到 2.26 秒，二次访问基本命中本地缓存。")

# ===================== 五、项目相关图片 =====================
h1("五、项目相关的图片")

body("以下四张图均截取自线上真实运行环境，未做界面替换或示意图处理。")
add_figure("01-登录页.png", "图 1　登录页：三端共用登录入口，可按身份切换", 11.6)
add_figure("02-管理工作台.png", "图 2　管理工作台：批次、农户、投诉与扫码量指标", 8.6)

doc.add_page_break()

add_figure("03-消费者溯源页.png", "图 3　消费者溯源页：可信指数、五维评分与证据展开", 8.6)
add_figure("04-AI消费助手问答.png", "图 4　AI 消费助手：回答附带知识库出处与工具调用记录", 8.6)

# ===================== 六、发表与获奖 =====================
h1("六、项目发表与获奖情况")

body("截至本次提交，本作品为团队首次参赛的原创作品，此前未在公开渠道发表，未参加其他赛事获奖，"
     "也未申请软件著作权登记。")
body("如后续完成软著登记、论文发表或其他赛事获奖，可在此处补充证书名称、编号与取得时间。")

# ===================== 七、优化说明 =====================
h1("七、项目的优化与演进说明")

body("本项目并非从零起步的全新构想，而是在团队既有的三端农产品溯源管理平台基础上做出的定位升级与功能重构，"
     "以下说明优化前的基础与本轮优化的具体落点。")

h2("（一）优化前的基础")
body("早期版本已经具备农户建档、经销商出入库、批次二维码和扫码查看报告的能力，本质上是一套面向经营者的溯源管理工具。"
     "它能回答“货从哪来”，但回答不了“该不该买”；消费者看完即走，平台也留不下任何关系。")

h2("（二）本轮优化的方向")
body("优化围绕三根支点展开：可验证的事实、可兑现的承诺、可运营的关系。"
     "事实侧把分散的检测与流转数据收敛成可解释的可信指数；承诺侧新增承诺发布与兑现核算；"
     "关系侧新增消费者身份、关注订阅与行为记录，使 AI 助手具备跨会话的上下文。")

h2("（三）优化的具体落点")
table_caption("表 4　优化前后的功能对照")
add_table(
    ["环节", "优化前", "优化后"],
    [
        ["溯源结果", "只能看到报告原文", "可信指数评分 + 逐条证据可展开"],
        ["消费者身份", "扫码记录匿名，无档案", "建立档案，支持关注、订阅与行为记录"],
        ["承诺管理", "无", "承诺发布、耗时核算、兑现公示"],
        ["投诉处理", "留言无时限、无进度", "受理时限、首次响应、超时标记"],
        ["AI 能力", "无，或固定文案", "智能体 + 工具调用 + 知识库检索 + 图片识别"],
        ["交付形态", "本地运行", "公网部署，单 Jar 交付，浏览器直接访问"],
    ],
    widths=[3.0, 6.0, 8.0],
)

h2("（四）优化后的结果")
body("经过本轮优化，系统已在公网稳定运行，四端功能全部可用，AI 接入真实模型并完成端到端验证，"
     "知识库条目达到 126 条，业务表扩展至 14 张。系统同时保留了单 Jar 交付、一条命令重启的轻量运维特性，"
     "便于在任何一台云主机上复现部署。")

doc.save(OUT)
print("OK", OUT)
