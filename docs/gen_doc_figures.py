# -*- coding: utf-8 -*-
"""生成技术文档配图（架构 / Agent 循环 / RAG 链路 / 可信指数 / 部署拓扑）。"""
import os

import matplotlib

matplotlib.use("Agg")
import matplotlib.pyplot as plt
from matplotlib.font_manager import FontProperties
from matplotlib.patches import FancyBboxPatch, FancyArrowPatch, Rectangle

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "figures")
os.makedirs(OUT, exist_ok=True)

CN_PATH = r"C:\Windows\Fonts\msyh.ttc"
CNB_PATH = r"C:\Windows\Fonts\msyhbd.ttc"
CN = FontProperties(fname=CN_PATH)
CNB = FontProperties(fname=CNB_PATH)

EDGE = "#4A5A6A"
FILL1 = "#EAF0F6"
FILL2 = "#F6F1E7"
FILL3 = "#EDF4EC"
FILL4 = "#F5EDED"


def new_axes(w, h):
    fig, ax = plt.subplots(figsize=(w, h))
    ax.set_xlim(0, 100)
    ax.set_ylim(0, 100)
    ax.axis("off")
    fig.subplots_adjust(left=0, right=1, top=1, bottom=0)
    return fig, ax


def box(ax, x, y, w, h, text, fc=FILL1, ec=EDGE, fs=6.4, bold=False, ls=1.35):
    p = FancyBboxPatch(
        (x, y), w, h,
        boxstyle="round,pad=0.25,rounding_size=1.6",
        linewidth=0.8, edgecolor=ec, facecolor=fc,
    )
    ax.add_patch(p)
    ax.text(x + w / 2, y + h / 2, text, ha="center", va="center",
            fontsize=fs, color="#1B1B1B", linespacing=ls,
            fontproperties=CNB if bold else CN)
    return p


def frame(ax, x, y, w, h, text=None, ec="#8A97A6", ls="-"):
    r = Rectangle((x, y), w, h, linewidth=0.9, edgecolor=ec, facecolor="none", linestyle=ls)
    ax.add_patch(r)
    if text:
        ax.text(x + 0.8, y + h + 1.6, text, ha="left", va="bottom",
                fontsize=6.6, color="#3A4654", fontproperties=CNB)
    return r


def arrow(ax, x1, y1, x2, y2, text=None, fs=5.9, rad=0.0, label_pos=0.5, dx=0, dy=1.6):
    a = FancyArrowPatch((x1, y1), (x2, y2), arrowstyle="-|>", mutation_scale=8,
                        linewidth=0.9, color=EDGE,
                        connectionstyle="arc3,rad=%s" % rad)
    ax.add_patch(a)
    if text:
        px = x1 + (x2 - x1) * label_pos + dx
        py = y1 + (y2 - y1) * label_pos + dy
        ax.text(px, py, text, ha="center", va="bottom", fontsize=fs,
                color="#3A4654", fontproperties=CN)


def save(fig, name):
    path = os.path.join(OUT, name)
    fig.savefig(path, dpi=220, facecolor="white", bbox_inches="tight", pad_inches=0.12)
    plt.close(fig)
    print("saved:", path)


# ---------------------------------------------------------------- 图 1 总体架构
def fig_architecture():
    fig, ax = new_axes(6.4, 4.5)

    frame(ax, 2, 79, 96, 19)
    ax.text(3, 99.2, "前端表现层 · 浏览器单页应用（Vue 3 + TypeScript + Vite）",
            fontsize=6.8, fontproperties=CNB, color="#3A4654")
    items = [
        (5, "消费者端\n溯源 / 决策翻译 / 助手"),
        (24, "农户端\n批次 / 农事 / 承诺"),
        (43, "经销商端\n入库 / 出库 / 流转"),
        (62, "管理员端\n工单 / 台账 / 大屏"),
        (81, "多模态输入\n拍照识别"),
    ]
    for x, t in items:
        box(ax, x, 81.5, 17, 12.5, t, fs=5.8)

    arrow(ax, 50, 79, 50, 70.5, "axios REST / SSE 流式 · 统一 {code, msg, data}", label_pos=0.5, dx=12, dy=0.4)

    frame(ax, 2, 27, 96, 43)
    ax.text(3, 71.2, "后端应用层 · Spring Boot 4 / Java 17（单服务部署）",
            fontsize=6.8, fontproperties=CNB, color="#3A4654")
    box(ax, 5, 60.5, 90, 8.5,
        "接口层：Controller / controller.api（公开溯源 · 消费者 · 农户 · 经销商 · 管理员 · AI）", fs=6.0)
    box(ax, 5, 50.5, 90, 8.0,
        "鉴权横切：TokenAuthFilter（X-Auth-Token 解析）→ LoginInterceptor（角色与路径拦截）",
        fc=FILL2, fs=6.0)
    box(ax, 5, 35.0, 90, 13.5,
        "服务层：AgentService（Agent 编排）· TrustScoreService（规则评分）· TrustPromiseService（承诺判定）\n"
        "KnowledgeService（向量检索）· ConsumerService（信任者关系）· AiSafetyService（输入拦截与输出兜底）",
        fs=6.0)
    box(ax, 5, 29.0, 90, 4.6, "数据访问层：MyBatis 注解式 Mapper · 统一软删除过滤 deleted = 0", fs=5.9)

    arrow(ax, 28, 27, 28, 21.5)
    arrow(ax, 72, 27, 72, 21.5, "外部调用")
    box(ax, 5, 8.5, 46, 12.5,
        "MySQL 8（utf8mb4）\n批次 / 农事 / 检测 / 流转 / 投诉 / 扫码日志\n信任者六表 / 知识库 126 条", fs=5.7)
    box(ax, 54, 8.5, 44, 12.5,
        "通义千问（DashScope）\nqwen-plus 对话 · qwen3-vl-plus 识图\ntext-embedding-v3 向量", fs=5.7, fc=FILL3)

    save(fig, "fig1_arch.png")


# ------------------------------------------------------------ 图 2 Agent 循环
def fig_agent_loop():
    fig, ax = new_axes(6.4, 3.6)

    box(ax, 2, 86, 44, 11, "① 用户提问：批次码 / 图片 / 消费者身份", fc=FILL2, fs=6.2)
    box(ax, 2, 71, 44, 11, "② 预取事实：批次 · 农事 · 检测 · 流转 · 知识", fs=6.2)
    box(ax, 2, 56, 44, 11, "③ 上下文组装：消费者画像 + 历史事件（≤20 条）", fs=6.2)
    box(ax, 2, 41, 44, 11, "④ 模型决策：function calling 判断是否需要工具", fc=FILL2, fs=6.2)
    box(ax, 2, 26, 44, 11, "⑤ 工具调用：批次 / 农事 / 检测 / 流转 /\n知识检索 / 常购清单 / 历史事件 / 当季推荐", fs=5.9)
    box(ax, 2, 11, 44, 11, "⑥ 结果回喂：工具返回值拼回上下文，不直接外露原始 JSON", fs=6.2)

    arrow(ax, 24, 86, 24, 82.2)
    arrow(ax, 24, 71, 24, 67.2)
    arrow(ax, 24, 56, 24, 52.2)
    arrow(ax, 24, 41, 24, 37.2)
    arrow(ax, 24, 26, 24, 22.2)

    box(ax, 54, 62, 44, 20,
        "⑦ 轮次判定\n仍需工具且轮次 < 5 → 回到 ④ 继续\n已收齐事实或达上限 → 强制收尾",
        fc=FILL2, fs=6.1)
    # 回环：沿两列之间的空隙上行，再进入轮次判定
    ax.plot([48.6, 48.6], [16.5, 68.5], linestyle="--", linewidth=0.9, color=EDGE)
    arrow(ax, 48.6, 68.5, 53.6, 68.5)
    arrow(ax, 76, 62, 76, 53, "收尾")
    box(ax, 54, 41, 44, 11, "⑧ 安全过滤：输入拦截 + 输出合规兜底", fc=FILL4, fs=6.2)
    arrow(ax, 76, 41, 76, 34.5)
    box(ax, 54, 20, 44, 14,
        "⑨ SSE 流式输出\n逐段推送 + 工具徽章 + 依据来源（批次事实 / 知识条目）", fc=FILL3, fs=6.0)

    ax.text(54, 9, "约束：MAX_ROUNDS = 5 · MAX_HISTORY = 20 · 数据缺失时明确说明，不以猜测填充",
            fontsize=5.9, color="#3A4654", fontproperties=CN, ha="left", va="center")

    save(fig, "fig2_agent.png")


# ------------------------------------------------------------- 图 3 RAG 链路
def fig_rag():
    fig, ax = new_axes(6.4, 3.1)

    box(ax, 1, 74, 20, 12, "用户问题\n（含批次上下文）", fs=6.0)
    arrow(ax, 21, 80, 25, 80)
    box(ax, 25, 74, 21, 12, "向量化\ntext-embedding-v3", fc=FILL2, fs=6.0)
    arrow(ax, 46, 80, 50, 80)
    box(ax, 50, 74, 20, 12, "余弦相似度\ntop-3 候选", fs=6.0)
    arrow(ax, 70, 80, 74, 80)
    box(ax, 74, 74, 24, 12, "相似度 ≥ 0.32 ？", fc=FILL2, fs=6.0)

    arrow(ax, 86, 74, 86, 66, "命中", fs=5.7, dx=8, dy=-3.0)
    box(ax, 62, 54, 36, 12, "注入依据：知识条目作为 sources\n随回答返回前端核验", fc=FILL3, fs=5.9)

    arrow(ax, 74, 78, 44, 64, "未命中", fs=5.7, rad=0.08, label_pos=0.5, dy=-3.2)
    box(ax, 2, 50, 40, 14, "关键词降级检索（标题 / 类别匹配）\n无结果则明确说明「该项暂无知识依据」", fs=5.9)
    arrow(ax, 42, 60, 62, 60, "命中则同样注入", fs=5.6, label_pos=0.5, dy=1.2)

    box(ax, 2, 22, 96, 18,
        "缓存与规模：向量结果在内存中缓存，仅当知识库条目发生变化时重建；\n"
        "知识库共 126 条（农事标准 102 + 消费科普 24），覆盖农残限量、安全间隔期、储存保鲜、清洗方式与选购建议",
        fc=FILL1, fs=5.8)

    save(fig, "fig3_rag.png")


# --------------------------------------------------------- 图 4 可信指数五维
def fig_trust_score():
    fig, ax = new_axes(6.4, 3.4)

    dims = [
        ("检测报告　权重 30", "有检测且合格 / 缺报告"),
        ("农事完整度　权重 20", "播种 · 施肥 · 打药 · 灌溉 各 5"),
        ("安全间隔期　权重 20", "≥ 7 天满分，3–6 天 12 分"),
        ("流转链路　权重 20", "采收 8 + 入库 6 + 出库 6"),
        ("新鲜度　权重 10", "采收至入库耗时分级"),
    ]
    y = 78
    for name, note in dims:
        box(ax, 2, y, 32, 13, name + "\n" + note, fc=FILL2, fs=5.7)
        y -= 16

    arrow(ax, 34, 49, 41, 49, "加权求和", fs=5.7, dy=1.4)
    box(ax, 41, 37, 24, 24, "可信指数\n0 – 100\n确定性规则，\n不调用大模型", fc=FILL3, fs=6.0, bold=True)
    arrow(ax, 65, 49, 71, 49, "按总分分级", fs=5.7, dy=2.4)

    for gy, lab, fc in [(74, "GREEN ≥ 80\n建议购买", FILL3),
                        (50, "YELLOW 60–79\n可以买，需核对细节", FILL2),
                        (26, "RED < 60\n建议谨慎，提示风险", FILL4)]:
        box(ax, 71, gy, 27, 14, lab, fc=fc, fs=5.9)

    ax.text(2, 6, "评分全部取自数据库真实记录，每个维度可展开查看原始证据（报告编号、农事时间、流转节点）；\n"
                  "数据缺失维度按缺省分计并在页面标注「该项暂无记录」，不以推测补分",
            fontsize=5.7, color="#3A4654", fontproperties=CN, ha="left", va="center", linespacing=1.5)

    save(fig, "fig4_score.png")


# -------------------------------------------------------------- 图 5 部署拓扑
def fig_deploy():
    fig, ax = new_axes(6.4, 2.8)

    frame(ax, 1, 24, 76, 70, "Docker Compose 一键部署（单机）· 本地开发可省略容器直接运行 jar", ls="--")

    box(ax, 5, 58, 24, 24, "浏览器\n消费者 / 农户 /\n经销商 / 管理员", fs=6.0)
    arrow(ax, 29, 70, 37, 70, "HTTP 8080\nREST + SSE", fs=5.6, dy=2.2)
    box(ax, 37, 54, 32, 32,
        "Spring Boot 容器\n内嵌 static/app 前端产物\nREST + SSE\n上传文件挂载 ./uploads",
        fc=FILL2, fs=5.8)
    arrow(ax, 53, 54, 53, 44, "JDBC 3306", fs=5.7, dx=8, dy=1.6)
    box(ax, 37, 30, 32, 14, "MySQL 8 容器\n库 nongyeplus（utf8mb4）", fc=FILL3, fs=5.8)

    arrow(ax, 69, 74, 80, 82, "HTTPS", fs=5.6, rad=-0.1)
    box(ax, 80, 74, 19, 20, "DashScope 云服务\nqwen-plus · qwen3-vl\nembedding\n（外网，不在容器内）", fc=FILL4, fs=5.2)

    ax.text(1, 12, "首次启动自动执行 sql/ 目录建表与种子脚本；AI Key 未配置时 AI 能力降级为规则文案，其余功能不受影响",
            fontsize=5.6, color="#3A4654", fontproperties=CN, ha="left", va="center")

    save(fig, "fig5_deploy.png")


if __name__ == "__main__":
    fig_architecture()
    fig_agent_loop()
    fig_rag()
    fig_trust_score()
    fig_deploy()
