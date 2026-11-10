package org.example.nongye.controller;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.example.nongye.entity.*;
import org.example.nongye.mapper.*;
import jakarta.servlet.http.HttpSession;
import org.example.nongye.service.AgentService;
import org.example.nongye.service.AiSafetyService;
import org.example.nongye.service.ConsumerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 问答：消费者扫码后基于批次数据回答问题
 * - 带图片 → 多模态：调通义千问 VL 模型识别照片（如农药包装、蔬菜状态）作答
 * - 纯文本 + 配置了 ai.api-key → Agent 模式：工具调用 + RAG 知识库 + 多轮会话
 * - 未配置 Key → 根据批次数据生成模拟回复（兜底，答辩演示够用）
 */
@RestController
@RequestMapping("/api/ai")
public class AiController {

    private static final Logger log = LoggerFactory.getLogger(AiController.class);

    @Autowired
    private CropBatchMapper batchMapper;
    @Autowired
    private FarmingLogMapper farmingLogMapper;
    @Autowired
    private TraceRecordMapper traceRecordMapper;
    @Autowired
    private DetectionReportMapper reportMapper;
    @Autowired
    private AgentService agentService;
    @Autowired
    private AiSafetyService safetyService;
    @Autowired
    private ConsumerService consumerService;

    @Value("${ai.api-key:}")
    private String apiKey;

    @Value("${ai.model:qwen-plus}")
    private String model;

    /** 多模态视觉模型（识别消费者上传的照片） */
    @Value("${ai.vl-model:qwen-vl-max-latest}")
    private String vlModel;

    /**
     * POST /api/ai/chat
     * body: {"batchId": 1, "question": "打了几次农药？", "sessionId": "xxx", "image": "data:image/jpeg;base64,..."}
     * 返回：{"reply": "...", "tools": [...], "sources": [{"type","label","detail"}]}
     */
    @PostMapping("/chat")
    public Map<String, Object> chat(@RequestBody Map<String, Object> body, HttpSession session) {
        Long batchId = body.get("batchId") != null ? Long.valueOf(body.get("batchId").toString()) : null;
        String question = body.get("question") != null ? body.get("question").toString().trim() : "";
        String image = body.get("image") != null ? body.get("image").toString().trim() : "";
        String sessionId = body.get("sessionId") != null ? body.get("sessionId").toString() : "default";

        Long consumerId = (Long) session.getAttribute("consumerId");

        Map<String, Object> result = new HashMap<>();
        if (question.isEmpty()) {
            result.put("reply", "请输入您的问题");
            result.put("tools", new ArrayList<>());
            result.put("sources", new ArrayList<>());
            return result;
        }

        CropBatch batch = null;
        if (batchId != null) {
            batch = batchMapper.selectById(batchId);
            if (batch == null) {
                result.put("reply", "批次信息不存在");
                result.put("tools", new ArrayList<>());
                result.put("sources", new ArrayList<>());
                return result;
            }
        }

        try {
            return answer(batch, question, image, sessionId, null, consumerId);
        } catch (Exception e) {
            log.error("AI 问答处理失败", e);
            result.put("reply", "AI 服务暂时不可用，请稍后再试");
            result.put("tools", new ArrayList<>());
            result.put("sources", new ArrayList<>());
            return result;
        }
    }

    /**
     * POST /api/ai/chat/stream
     * SSE 推送：{"type":"step","label","summary"} → {"type":"reply","reply","tools","sources"}
     * 前端据此实时展示"正在查检测报告…"等步骤，最后拿到完整回答与依据。
     */
    @PostMapping(value = "/chat/stream", produces = "text/event-stream;charset=UTF-8")
    public SseEmitter chatStream(@RequestBody Map<String, Object> body, HttpSession session) {
        SseEmitter emitter = new SseEmitter(180000L);
        Long batchId = body.get("batchId") != null ? Long.valueOf(body.get("batchId").toString()) : null;
        String question = body.get("question") != null ? body.get("question").toString().trim() : "";
        String image = body.get("image") != null ? body.get("image").toString().trim() : "";
        String sessionId = body.get("sessionId") != null ? body.get("sessionId").toString() : "default";

        Long consumerId = (Long) session.getAttribute("consumerId");
        final Long finalBatchId = batchId;

        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                if (question.isEmpty()) {
                    sendEvent(emitter, Map.of("type", "reply", "reply", "请输入您的问题",
                            "tools", List.of(), "sources", List.of()));
                    emitter.complete();
                    return;
                }
                CropBatch batch = null;
                if (finalBatchId != null) {
                    batch = batchMapper.selectById(finalBatchId);
                    if (batch == null) {
                        sendEvent(emitter, Map.of("type", "reply", "reply", "批次信息不存在",
                                "tools", List.of(), "sources", List.of()));
                        emitter.complete();
                        return;
                    }
                }
                sendEvent(emitter, Map.of("type", "step", "label", "理解问题", "summary", "正在分析您的问题"));
                if (!image.isEmpty() && apiKey != null && !apiKey.isEmpty()) {
                    sendEvent(emitter, Map.of("type", "step", "label", "图片识别", "summary", "正在识别您上传的照片"));
                }
                CropBatch finalBatch = batch;
                Map<String, Object> result = answer(finalBatch, question, image, sessionId,
                        new AgentService.ToolSink() {
                            @Override
                            public void onTool(String label, String summary) {
                                sendEvent(emitter, Map.of("type", "step", "label", label, "summary", summary));
                            }

                            @Override
                            public void onToken(String delta) {
                                sendEvent(emitter, Map.of("type", "delta", "delta", delta));
                            }
                        }, consumerId);
                sendEvent(emitter, Map.of("type", "reply",
                        "reply", result.get("reply"),
                        "tools", result.getOrDefault("tools", List.of()),
                        "sources", result.getOrDefault("sources", List.of())));
                emitter.complete();
            } catch (Exception e) {
                log.error("AI 流式问答失败", e);
                try {
                    sendEvent(emitter, Map.of("type", "reply", "reply", "AI 服务暂时不可用，请稍后再试",
                            "tools", List.of(), "sources", List.of()));
                } catch (Exception ignored) {
                    // 连接已断开
                }
                emitter.complete();
            }
        });
        return emitter;
    }

    /**
     * POST /api/ai/health-report
     * body: {"batchId": 1}
     * 返回：{"report": "...", "trust": {...}, "generatedAt": "..."}
     * 事实基础 = TrustScoreService 确定性评分（可信指数），模型仅做可读化转写；出口统一过安全过滤。
     */
    @PostMapping("/health-report")
    public Map<String, Object> healthReport(@RequestBody Map<String, Object> body) {
        Long batchId = body.get("batchId") != null ? Long.valueOf(body.get("batchId").toString()) : null;
        Map<String, Object> result = new HashMap<>();
        if (batchId == null) {
            result.put("report", "缺少批次信息");
            return result;
        }
        try {
            return agentService.healthReport(batchId);
        } catch (Exception e) {
            log.error("体检报告生成失败", e);
            result.put("report", "AI 服务暂时不可用，请稍后再试");
            return result;
        }
    }

    /** 统一的回答生成：有 Key 时 Agent（可选融合图片识别），无 Key 时按批次数据兜底 */
    @SuppressWarnings("unchecked")
    private Map<String, Object> answer(CropBatch batch, String question, String image,
                                       String sessionId, AgentService.ToolSink sink, Long consumerId) {
        Map<String, Object> out = new HashMap<>();
        List<Map<String, Object>> sources = new ArrayList<>();

        // 输入安全校验：提示注入、超长、造假/规避检测请求
        String blocked = safetyService.checkInput(question);
        if (blocked != null) {
            out.put("reply", blocked);
            out.put("tools", new ArrayList<>());
            out.put("sources", sources);
            return out;
        }

        if (apiKey != null && !apiKey.isEmpty()) {
            String enriched = question;
            if (batch != null && !image.isEmpty()) {
                // 多模态融合：先用 VL 识别图片，再把识别结果作为上下文交给 Agent 查批次数据/知识库
                String vision = callVlApi(batch, question, image);
                enriched = question + "\n\n【图片识别结果（供参考）】\n" + vision;
                sources.add(sourceItem("image", "图片识别", abbreviate(vision, 60)));
            }
            Long batchId = batch != null ? batch.getId() : null;
            Map<String, Object> agentResult = agentService.chat(batchId, sessionId, enriched, consumerId, sink);
            sources.addAll((List<Map<String, Object>>) agentResult.getOrDefault("sources", List.of()));
            out.put("reply", safetyService.guardOutput(String.valueOf(agentResult.get("reply"))));
            out.put("tools", agentResult.get("tools"));
            out.put("sources", sources);
            return out;
        }

        // 无 Key：按批次数据生成兜底回答
        if (batch == null) {
            out.put("reply", "请先关联一个批次再提问，或者描述您对哪个商品有疑问。");
            out.put("tools", new ArrayList<>());
            out.put("sources", sources);
            return out;
        }
        List<FarmingLog> logs = farmingLogMapper.selectByBatchId(batch.getId());
        List<TraceRecord> records = traceRecordMapper.selectByBatchId(batch.getId());
        List<DetectionReport> reports = reportMapper.selectByBatchId(batch.getId());
        String reply = mockReply(question, batch, logs, records, reports);
        if (!image.isEmpty()) {
            reply = "（图片识别需配置 AI 服务，以下按文字问题回答）\n\n" + reply;
        }
        sources.add(sourceItem("data", "批次信息", batch.getCropName() + "（" + batch.getBatchNo() + "）"));
        sources.add(sourceItem("record", "农事记录", logs.size() + " 条"));
        if (!reports.isEmpty()) {
            sources.add(sourceItem("report", "检测报告",
                    reports.size() + " 份 · 最新 " + reports.get(0).getResult()));
        }
        sources.add(sourceItem("trace", "流转记录", records.size() + " 条"));
        out.put("reply", safetyService.guardOutput(reply));
        out.put("tools", new ArrayList<>());
        out.put("sources", sources);
        return out;
    }

    private Map<String, Object> sourceItem(String type, String label, String detail) {
        Map<String, Object> item = new HashMap<>();
        item.put("type", type);
        item.put("label", label);
        item.put("detail", detail);
        return item;
    }

    private String abbreviate(String text, int max) {
        if (text == null) return "";
        String oneLine = text.replace("\n", " ").trim();
        return oneLine.length() <= max ? oneLine : oneLine.substring(0, max) + "…";
    }

    private void sendEvent(SseEmitter emitter, Map<String, Object> payload) {
        try {
            emitter.send(SseEmitter.event().data(payload));
        } catch (Exception e) {
            // 客户端断开时忽略，避免刷日志
        }
    }

    /** 多模态：调通义千问 VL（图片以 data URL 传入，OpenAI 兼容格式） */
    private String callVlApi(CropBatch batch, String question, String imageDataUrl) {
        // 图片转文字说明 + 批次上下文一起喂给模型
        String context = buildBriefContext(batch);

        JSONArray userContent = new JSONArray();
        userContent.add(new JSONObject().set("type", "text")
                .set("text", "消费者在溯源页上传了一张照片并提问：\n" + question
                        + "\n\n" + context
                        + "\n请先客观描述图片中可见的内容（作物/包装/标签文字），再针对问题给出判断或建议。"
                        + "看不清或无法确认的地方必须明说，不要猜测。用中文分点回答，控制在 200 字以内。"));
        userContent.add(new JSONObject().set("type", "image_url")
                .set("image_url", new JSONObject().set("url", imageDataUrl)));

        JSONObject body = new JSONObject();
        body.set("model", vlModel);
        body.set("messages", new Object[]{
                new JSONObject().set("role", "user").set("content", userContent)
        });

        try {
            HttpResponse resp = HttpRequest.post("https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .body(body.toString())
                    .timeout(60000)
                    .execute();
            if (resp.isOk()) {
                return JSONUtil.parseObj(resp.body())
                        .getJSONArray("choices").getJSONObject(0)
                        .getJSONObject("message").getStr("content", "");
            }
            log.error("VL 模型返回非 200：{}", resp.body());
            return "图片识别服务暂时不可用，请稍后再试";
        } catch (Exception e) {
            log.error("调用 VL 模型失败", e);
            return "图片识别服务暂时不可用，请稍后再试";
        }
    }

    /** 批次简要上下文（多模态用，比完整版省 token） */
    private String buildBriefContext(CropBatch batch) {
        List<FarmingLog> logs = farmingLogMapper.selectByBatchId(batch.getId());
        List<DetectionReport> reports = reportMapper.selectByBatchId(batch.getId());
        StringBuilder sb = new StringBuilder();
        sb.append("该批次信息：作物=").append(batch.getCropName())
          .append("，产地=").append(batch.getLandLocation() == null ? "未记录" : batch.getLandLocation())
          .append("，采收日期=").append(batch.getActualHarvestDate() == null ? "未采收" : batch.getActualHarvestDate())
          .append("。\n");
        if (!logs.isEmpty()) {
            sb.append("近期农事记录：\n");
            int limit = Math.min(logs.size(), 5);
            for (int i = logs.size() - limit; i < logs.size(); i++) {
                FarmingLog l = logs.get(i);
                sb.append("- ").append(l.getLogDate()).append(" ").append(l.getMaterial() == null ? "" : l.getMaterial());
                sb.append("\n");
            }
        }
        if (!reports.isEmpty()) {
            sb.append("最新检测结论：").append(reports.get(0).getResult())
              .append("（").append(reports.get(0).getInstitution()).append("）\n");
        }
        return sb.toString();
    }

    /** 模拟回复：根据问题关键词匹配批次数据（无 API Key 时兜底） */
    private String mockReply(String question, CropBatch batch,
                              List<FarmingLog> logs, List<TraceRecord> records,
                              List<DetectionReport> reports) {
        String q = question.toLowerCase();

        // 农药相关
        if (q.contains("农药") || q.contains("打药") || q.contains("残留")) {
            long count = logs.stream().filter(l -> "PESTICIDE".equals(l.getLogType())).count();
            if (count == 0) {
                return "在该批次已登记的农事记录中，暂未查到打药记录。"
                        + "由于记录可能不完整，不能据此判定为“生态种植”或“无农药”；"
                        + (reports.isEmpty() ? "该批次也暂无检测报告，建议以收到的农残检测结果为准。"
                                             : "可参考检测报告结论：" + reports.get(0).getResult() + "。");
            }
            StringBuilder sb = new StringBuilder("该批次共打药 " + count + " 次：\n");
            logs.stream().filter(l -> "PESTICIDE".equals(l.getLogType())).forEach(l -> {
                sb.append("• ").append(l.getLogDate());
                if (l.getMaterial() != null) sb.append(" 使用 ").append(l.getMaterial());
                if (l.getDosage() != null) sb.append("（").append(l.getDosage()).append("）");
                sb.append("\n");
            });
            if (!reports.isEmpty()) {
                DetectionReport r = reports.get(0);
                sb.append("检测报告结果：").append(r.getResult()).append("（").append(r.getInstitution()).append("）");
            }
            return sb.toString();
        }

        // 肥料相关
        if (q.contains("肥") || q.contains("施肥")) {
            long count = logs.stream().filter(l -> "FERTILIZE".equals(l.getLogType())).count();
            if (count == 0) return "该批次没有施肥记录。";
            StringBuilder sb = new StringBuilder("该批次共施肥 " + count + " 次：\n");
            logs.stream().filter(l -> "FERTILIZE".equals(l.getLogType())).forEach(l -> {
                sb.append("• ").append(l.getLogDate());
                if (l.getMaterial() != null) sb.append(" 使用 ").append(l.getMaterial());
                if (l.getDosage() != null) sb.append("（").append(l.getDosage()).append("）");
                sb.append("\n");
            });
            return sb.toString();
        }

        // 检测相关
        if (q.contains("检测") || q.contains("安全") || q.contains("合格")) {
            if (reports.isEmpty()) return "该批次暂无检测报告。";
            StringBuilder sb = new StringBuilder("检测报告信息：\n");
            for (DetectionReport r : reports) {
                sb.append("• 报告编号：").append(r.getReportNo()).append("\n");
                sb.append("• 检测机构：").append(r.getInstitution()).append("\n");
                sb.append("• 检测日期：").append(r.getReportDate()).append("\n");
                sb.append("• 检测结果：").append(r.getResult()).append("\n");
            }
            return sb.toString();
        }

        // 保存/储存
        if (q.contains("保存") || q.contains("储存") || q.contains("保鲜")) {
            String crop = batch.getCropName();
            if (crop.contains("西红柿") || crop.contains("番茄")) {
                return crop + "保存建议：未切开的常温放 3-5 天，放冰箱冷藏可存 1 周。切开后需密封冷藏并尽快食用。";
            }
            if (crop.contains("黄瓜")) {
                return crop + "保存建议：用保鲜膜包好放冰箱冷藏，可存 3-5 天。避免与乙烯含量高的水果同放。";
            }
            return crop + "保存建议：建议放冰箱冷藏保存，尽快食用保证新鲜。";
        }

        // 产地/哪里
        if (q.contains("产地") || q.contains("哪里") || q.contains("哪产") || q.contains("谁种")) {
            StringBuilder sb = new StringBuilder();
            sb.append("产地：").append(batch.getLandLocation() != null ? batch.getLandLocation() : "未记录").append("\n");
            sb.append("作物：").append(batch.getCropName());
            if (batch.getVariety() != null) sb.append("（").append(batch.getVariety()).append("）");
            sb.append("\n");
            sb.append("播种日期：").append(batch.getPlantDate());
            return sb.toString();
        }

        // 采收
        if (q.contains("采收") || q.contains("收获") || q.contains("摘")) {
            if (batch.getActualHarvestDate() == null) return "该批次尚未采收。";
            StringBuilder sb = new StringBuilder();
            sb.append("采收日期：").append(batch.getActualHarvestDate()).append("\n");
            if (batch.getHarvestWeight() != null) sb.append("采收重量：").append(batch.getHarvestWeight()).append("斤");
            return sb.toString();
        }

        // 流转/运输
        if (q.contains("运输") || q.contains("流转") || q.contains("物流") || q.contains("从哪")) {
            if (records.isEmpty()) return "暂无流转记录。";
            StringBuilder sb = new StringBuilder("该批次的流转路径：\n");
            for (TraceRecord r : records) {
                sb.append("• ").append(traceTypeName(r.getType()));
                if (r.getLocation() != null) sb.append("，").append(r.getLocation());
                if (r.getRemark() != null) sb.append("（").append(r.getRemark()).append("）");
                sb.append("\n");
            }
            return sb.toString();
        }

        // 默认：总结批次信息
        StringBuilder sb = new StringBuilder();
        sb.append("这是关于「").append(batch.getCropName()).append("」的溯源信息：\n");
        sb.append("• 产地：").append(batch.getLandLocation() != null ? batch.getLandLocation() : "未记录").append("\n");
        sb.append("• 播种：").append(batch.getPlantDate()).append("\n");
        if (batch.getActualHarvestDate() != null) {
            sb.append("• 采收：").append(batch.getActualHarvestDate());
            if (batch.getHarvestWeight() != null) sb.append("，").append(batch.getHarvestWeight()).append("斤");
            sb.append("\n");
        }
        sb.append("• 农事记录 ").append(logs.size()).append(" 条");
        if (!reports.isEmpty()) sb.append("，检测").append(reports.get(0).getResult());
        sb.append("\n\n您可以问我：打了几次农药？怎么保存？检测结果怎么样？");
        return sb.toString();
    }

    private String traceTypeName(String type) {
        return switch (type) {
            case "HARVEST" -> "采收";
            case "INBOUND" -> "入库";
            case "OUTBOUND" -> "出库";
            case "SALE" -> "销售";
            default -> type;
        };
    }
}
