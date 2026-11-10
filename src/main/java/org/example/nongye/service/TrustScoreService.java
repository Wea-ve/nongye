package org.example.nongye.service;

import org.example.nongye.entity.*;
import org.example.nongye.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 可信指数评分（确定性规则引擎，非 LLM 打分，保证可复核）
 * 五个维度：检测报告 30 / 农事完整度 20 / 安全间隔期 20 / 流转完整性 20 / 数据新鲜度 10
 * 每个维度输出 score/max/level 及 evidence 证据列表（引用真实记录号与日期），前端可展开核验。
 * 等级：>=80 GREEN（可信）/ >=60 YELLOW（基本可信）/ 其余 RED（需谨慎）
 */
@Service
public class TrustScoreService {

    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Autowired
    private CropBatchMapper batchMapper;
    @Autowired
    private FarmingLogMapper farmingLogMapper;
    @Autowired
    private TraceRecordMapper traceRecordMapper;
    @Autowired
    private DetectionReportMapper reportMapper;

    /** 对外评分入口：返回 {score, level, verdict, items[], generatedAt} */
    public Map<String, Object> evaluate(Long batchId) {
        CropBatch batch = batchMapper.selectById(batchId);
        if (batch == null) {
            return null;
        }
        List<FarmingLog> logs = farmingLogMapper.selectByBatchId(batchId);
        List<DetectionReport> reports = reportMapper.selectByBatchId(batchId);
        List<TraceRecord> records = traceRecordMapper.selectByBatchId(batchId);

        List<Map<String, Object>> items = new ArrayList<>();
        items.add(scoreDetection(reports));
        items.add(scoreFarming(logs));
        items.add(scoreInterval(logs, batch));
        items.add(scoreTrace(records, batch));
        items.add(scoreFreshness(logs, records, reports));

        int total = 0;
        for (Map<String, Object> it : items) {
            total += (int) it.get("score");
        }
        String level = total >= 80 ? "GREEN" : total >= 60 ? "YELLOW" : "RED";
        String verdict;
        switch (level) {
            case "GREEN" -> verdict = "该批次检测合格、关键农事记录齐全、流转链条清晰，整体可信度高";
            case "YELLOW" -> verdict = "该批次数据基本可信，但存在待补全或待观察项，建议结合下方明细判断";
            default -> verdict = "该批次存在关键信息缺失或风险项，建议谨慎采信并优先核实检测与用药信息";
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("score", total);
        result.put("level", level);
        result.put("verdict", verdict);
        result.put("items", items);
        result.put("generatedAt", LocalDateTime.now().format(DTF));
        return result;
    }

    /** 维度一：第三方检测报告（30 分） */
    private Map<String, Object> scoreDetection(List<DetectionReport> reports) {
        List<String> evidence = new ArrayList<>();
        for (DetectionReport r : reports) {
            String date = r.getReportDate() != null ? r.getReportDate().format(DF) : "未知日期";
            evidence.add((r.getReportNo() != null ? r.getReportNo() : "检测报告")
                    + " · " + (r.getInstitution() != null ? r.getInstitution() : "检测机构")
                    + " · " + date + " · 结论：" + (r.getResult() != null ? r.getResult() : "未填写"));
        }
        boolean qualified = reports.stream().anyMatch(r -> "合格".equals(r.getResult()));
        if (qualified) {
            return item("detection", "第三方检测", 30, 30, "GREEN",
                    "已有检测结论为「合格」的第三方报告", evidence);
        }
        if (!reports.isEmpty()) {
            return item("detection", "第三方检测", 5, 30, "RED",
                    "已有检测报告但结论均非「合格」，存在农残超标风险", evidence);
        }
        evidence.add("尚未上传任何检测报告");
        return item("detection", "第三方检测", 0, 30, "YELLOW",
                "尚未上传第三方检测报告，无法核验农残结果", evidence);
    }

    /** 维度二：关键农事记录完整度（20 分，播种/施肥/打药/灌溉各 5 分） */
    private Map<String, Object> scoreFarming(List<FarmingLog> logs) {
        String[] keys = {"SOWING", "FERTILIZE", "PESTICIDE", "IRRIGATE"};
        String[] names = {"播种", "施肥", "打药", "灌溉"};
        int score = 0;
        List<String> evidence = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        for (int i = 0; i < keys.length; i++) {
            final String key = keys[i];
            final String name = names[i];
            FarmingLog hit = logs.stream().filter(l -> key.equals(l.getLogType()))
                    .min((a, b) -> b.getLogDate().compareTo(a.getLogDate())).orElse(null);
            if (hit != null) {
                score += 5;
                evidence.add(name + "记录 · " + hit.getLogDate().format(DF)
                        + (hit.getMaterial() != null && !hit.getMaterial().isEmpty()
                        ? " · " + hit.getMaterial() + (hit.getDosage() != null ? "（" + hit.getDosage() + "）" : "") : ""));
            } else {
                missing.add(name);
            }
        }
        evidence.addAll(missing.stream().map(m -> "缺少" + m + "记录").toList());
        String level = score >= 15 ? "GREEN" : score >= 10 ? "YELLOW" : "RED";
        String detail = missing.isEmpty() ? "播种 / 施肥 / 打药 / 灌溉关键记录齐全"
                : "缺少 " + String.join("、", missing) + " 记录，种植过程信息不完整";
        return item("farming", "农事记录完整度", score, 20, level, detail, evidence);
    }

    /** 维度三：农药安全间隔期合规（20 分，最后打药距采收/今日 ≥7 天为达标） */
    private Map<String, Object> scoreInterval(List<FarmingLog> logs, CropBatch batch) {
        LocalDate last = logs.stream().filter(l -> "PESTICIDE".equals(l.getLogType()))
                .map(FarmingLog::getLogDate).max(LocalDate::compareTo).orElse(null);
        if (last == null) {
            List<String> ev = new ArrayList<>();
            ev.add("无任何打药记录，无法核验安全间隔期（注意：无记录不等于未用药，也不等于生态种植）");
            return item("interval", "安全间隔期", 10, 20, "YELLOW",
                    "无打药记录可查，无法证明用药合规，建议要求农户补录", ev);
        }
        boolean harvested = batch.getActualHarvestDate() != null;
        LocalDate baseline = harvested ? batch.getActualHarvestDate() : LocalDate.now();
        long gap = ChronoUnit.DAYS.between(last, baseline);
        String baseName = harvested ? "采收" : "今日";
        List<String> ev = new ArrayList<>();
        ev.add("最后打药 " + last.format(DF) + "，距" + baseName + " " + gap + " 天"
                + (harvested ? "（采收日 " + baseline.format(DF) + "）" : ""));
        if (gap < 0) {
            ev.add("采收日之后仍存在打药记录，属异常情形");
            return item("interval", "安全间隔期", 0, 20, "RED",
                    "采收后仍有打药记录，严重违反用药规范", ev);
        }
        if (gap >= 7) {
            ev.add("满足 ≥7 天安全间隔期要求");
            return item("interval", "安全间隔期", 20, 20, "GREEN",
                    "最后一次用药距" + baseName + "已超过 7 天安全间隔期", ev);
        }
        if (gap >= 3) {
            return item("interval", "安全间隔期", 12, 20, "YELLOW",
                    "用药间隔 " + gap + " 天，未满 7 天安全间隔期，建议关注", ev);
        }
        return item("interval", "安全间隔期", 0, 20, "RED",
                "用药间隔仅 " + gap + " 天，低于 7 天安全间隔期要求", ev);
    }

    /** 维度四：流转链路完整性（20 分：采收 8 + 入库 6 + 出库/销售 6） */
    private Map<String, Object> scoreTrace(List<TraceRecord> records, CropBatch batch) {
        List<String> evidence = new ArrayList<>();
        for (TraceRecord r : records) {
            String t = r.getOperateTime() != null ? r.getOperateTime().format(DF) : "-";
            String qty = r.getQuantity() != null ? " · " + r.getQuantity() + " 斤" : "";
            evidence.add((r.getTypeName() != null ? r.getTypeName() : recordTypeName(r.getType()))
                    + " · " + (r.getFromUserName() != null ? r.getFromUserName() : "?")
                    + " → " + (r.getToUserName() != null ? r.getToUserName() : "市场/消费者")
                    + " · " + t + qty
                    + (r.getLocation() != null && !r.getLocation().isEmpty() ? " · " + r.getLocation() : ""));
        }
        int score = 0;
        if (records.stream().anyMatch(r -> "HARVEST".equals(r.getType()))) score += 8;
        if (records.stream().anyMatch(r -> "INBOUND".equals(r.getType()))) score += 6;
        if (records.stream().anyMatch(r -> "OUTBOUND".equals(r.getType()) || "SALE".equals(r.getType()))) score += 6;
        String level = score >= 14 ? "GREEN" : score >= 8 ? "YELLOW" : "RED";
        String detail;
        if (score == 0 && "PLANTING".equals(batch.getStatus())) {
            level = "YELLOW";
            detail = "作物尚在种植期，尚未进入采收与流通环节";
        } else if (score >= 14) {
            detail = "采收 → 入库 → 出库链路完整，经手主体可追溯";
        } else {
            detail = "流转链路不完整，缺少关键环节记录";
        }
        if (evidence.isEmpty()) {
            evidence.add("暂无任何流转节点记录");
        }
        return item("trace", "流转链路完整性", score, 20, level, detail, evidence);
    }

    /** 维度五：数据新鲜度（10 分，最近一次记录更新时间） */
    private Map<String, Object> scoreFreshness(List<FarmingLog> logs, List<TraceRecord> records,
                                               List<DetectionReport> reports) {
        LocalDate latest = null;
        for (FarmingLog l : logs) {
            if (l.getLogDate() != null && (latest == null || l.getLogDate().isAfter(latest))) latest = l.getLogDate();
        }
        for (DetectionReport r : reports) {
            if (r.getReportDate() != null && (latest == null || r.getReportDate().isAfter(latest))) latest = r.getReportDate();
        }
        LocalDateTime latestDt = null;
        for (TraceRecord r : records) {
            if (r.getOperateTime() != null && (latestDt == null || r.getOperateTime().isAfter(latestDt))) latestDt = r.getOperateTime();
        }
        if (latestDt != null && (latest == null || latestDt.toLocalDate().isAfter(latest))) latest = latestDt.toLocalDate();

        if (latest == null) {
            List<String> ev = new ArrayList<>();
            ev.add("暂无任何带日期的记录");
            return item("freshness", "数据新鲜度", 5, 10, "YELLOW", "暂无记录，无法评估数据时效", ev);
        }
        long days = ChronoUnit.DAYS.between(latest, LocalDate.now());
        List<String> ev = new ArrayList<>();
        ev.add("最近一次记录更新于 " + latest.format(DF) + "（" + days + " 天前）");
        int score;
        String level;
        if (days <= 7) {
            score = 10;
            level = "GREEN";
        } else if (days <= 30) {
            score = 7;
            level = "YELLOW";
        } else if (days <= 90) {
            score = 4;
            level = "YELLOW";
        } else {
            score = 2;
            level = "RED";
        }
        return item("freshness", "数据新鲜度", score, 10, level,
                days <= 30 ? "数据更新及时" : "数据已 " + days + " 天未更新，时效性下降", ev);
    }

    /** 统一构造维度明细项 */
    private Map<String, Object> item(String key, String label, int score, int max,
                                     String level, String detail, List<String> evidence) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("key", key);
        m.put("label", label);
        m.put("score", score);
        m.put("max", max);
        m.put("level", level);
        m.put("detail", detail);
        m.put("evidence", evidence);
        return m;
    }

    private String recordTypeName(String type) {
        if (type == null) return "流转";
        return switch (type) {
            case "HARVEST" -> "采收";
            case "INBOUND" -> "入库";
            case "OUTBOUND" -> "出库";
            case "SALE" -> "销售";
            default -> type;
        };
    }
}
