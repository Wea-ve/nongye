package org.example.nongye.service;

import org.example.nongye.entity.*;
import org.example.nongye.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 信任承诺（TrustPromise）服务：创建承诺、查询承诺、兑现展示、承诺打分
 */
@Service
public class TrustPromiseService {

    @Autowired
    private TrustPromiseMapper trustPromiseMapper;
    @Autowired
    private TraceRecordMapper traceRecordMapper;
    @Autowired
    private FulfillmentRecordMapper fulfillmentRecordMapper;
    @Autowired
    private CropBatchMapper cropBatchMapper;
    @Autowired
    private ComplaintMapper complaintMapper;
    @Autowired
    private DetectionReportMapper reportMapper;

    /**
     * 新建承诺
     * @param type         承诺类型（对应实体 promiseType，如 FRESH/HARVEST_DATE/QUALITY）
     * @param value        承诺值（对应实体 promiseValue）
     * @param evidencePath 证据文件路径
     */
    public TrustPromise create(Long batchId, Long farmerId, String type, String value, String evidencePath) {
        TrustPromise p = new TrustPromise();
        p.setBatchId(batchId);
        p.setFarmerId(farmerId);
        p.setPromiseType(type);
        p.setPromiseValue(value);
        p.setEvidencePath(evidencePath);
        p.setStatus("ACTIVE");
        p.setCreateTime(LocalDateTime.now());
        trustPromiseMapper.insert(p);
        return p;
    }

    /** 批次下所有承诺 */
    public List<TrustPromise> listByBatch(Long batchId) {
        return trustPromiseMapper.selectByBatch(batchId);
    }

    /**
     * 兑现展示：promises + actualTimes(HARVEST/INBOUND/OUTBOUND 实际时间) + score(0-10)。
     * score 简化按 promise 数量/3 封顶 10，后续可替换 computePromiseScore。
     */
    public Map<String, Object> fulfillment(Long batchId) {
        List<TrustPromise> promises = trustPromiseMapper.selectByBatch(batchId);
        List<TraceRecord> traces = traceRecordMapper.selectByBatchId(batchId);
        List<FulfillmentRecord> fulfillments = fulfillmentRecordMapper.selectByBatch(batchId);
        CropBatch batch = cropBatchMapper.selectById(batchId);

        // 找各节点第一次出现时间（actualTimes）
        Map<String, LocalDateTime> actualTimes = new LinkedHashMap<>();
        for (String type : Arrays.asList("HARVEST", "INBOUND", "OUTBOUND", "SALE")) {
            traces.stream()
                    .filter(t -> type.equals(t.getType()) && t.getOperateTime() != null)
                    .map(TraceRecord::getOperateTime)
                    .min(LocalDateTime::compareTo)
                    .ifPresent(t -> actualTimes.put(type, t));
        }

        // judgments（保留）
        LocalDateTime harvestTime = actualTimes.get("HARVEST");
        LocalDateTime inboundTime = actualTimes.get("INBOUND");
        List<Map<String, Object>> judgments = promises.stream().map(p -> {
            Map<String, Object> j = new LinkedHashMap<>();
            j.put("promise", p);
            String type = p.getPromiseType();
            String status = "UNKNOWN";
            String remark = "无足够流转数据";

            if ("FRESH".equalsIgnoreCase(type)) {
                int promisedHours = parseHours(p.getPromiseValue());
                if (promisedHours > 0 && harvestTime != null && inboundTime != null) {
                    long actualHours = Duration.between(harvestTime, inboundTime).toHours();
                    boolean ok = actualHours <= promisedHours;
                    status = ok ? "FULFILLED" : "FAILED";
                    remark = String.format("承诺≤%dh，实际 HARVEST→INBOUND 耗时 %dh", promisedHours, actualHours);
                }
            } else if ("HARVEST_DATE".equalsIgnoreCase(type)) {
                if (batch != null && batch.getExpectHarvestDate() != null && batch.getActualHarvestDate() != null) {
                    long diff = java.time.temporal.ChronoUnit.DAYS.between(
                            batch.getExpectHarvestDate(), batch.getActualHarvestDate());
                    boolean ok = Math.abs(diff) <= 3;
                    status = ok ? "FULFILLED" : "FAILED";
                    remark = String.format("承诺 %s，实际 %s（差 %d 天）",
                            batch.getExpectHarvestDate(), batch.getActualHarvestDate(), diff);
                }
            } else if ("QUALITY".equalsIgnoreCase(type)) {
                List<DetectionReport> reports = reportMapper.selectByBatchId(batchId);
                if (!reports.isEmpty()) {
                    boolean anyQualified = reports.stream().anyMatch(r -> "合格".equals(r.getResult()));
                    status = anyQualified ? "FULFILLED" : "FAILED";
                    DetectionReport r = reports.get(0);
                    remark = "已附第三方检测报告：" + (r.getReportNo() == null ? "报告" : r.getReportNo())
                            + "（" + (r.getInstitution() == null ? "检测机构" : r.getInstitution()) + "，"
                            + (r.getResult() == null ? "结果未知" : r.getResult()) + "）";
                } else {
                    remark = "暂无检测报告支撑该承诺";
                }
            }

            j.put("judgment", status);
            j.put("remark", remark);
            return j;
        }).collect(Collectors.toList());

        // 兑现得分：已兑现条数 / 总条数 * 10
        int score = computePromiseScore(batchId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("batch", batch);
        result.put("promises", promises);
        result.put("actualTimes", actualTimes);
        result.put("traces", traces);
        result.put("fulfillments", fulfillments);
        result.put("judgments", judgments);
        result.put("score", score);
        return result;
    }

    /**
     * 计算承诺兑现附加分（0-10）：兑现比例 * 10，至少有一条承诺才计分。
     */
    public int computePromiseScore(Long batchId) {
        List<TrustPromise> promises = trustPromiseMapper.selectByBatch(batchId);
        if (promises == null || promises.isEmpty()) {
            return 0;
        }
        List<TraceRecord> traces = traceRecordMapper.selectByBatchId(batchId);
        CropBatch batch = cropBatchMapper.selectById(batchId);

        int fulfilled = 0;
        for (TrustPromise p : promises) {
            String type = p.getPromiseType();
            boolean ok = false;
            if ("FRESH".equalsIgnoreCase(type)) {
                int promisedHours = parseHours(p.getPromiseValue());
                LocalDateTime harvestTime = traces.stream()
                        .filter(t -> "HARVEST".equals(t.getType()))
                        .map(TraceRecord::getOperateTime).filter(Objects::nonNull)
                        .min(LocalDateTime::compareTo).orElse(null);
                LocalDateTime inboundTime = traces.stream()
                        .filter(t -> "INBOUND".equals(t.getType()))
                        .map(TraceRecord::getOperateTime).filter(Objects::nonNull)
                        .min(LocalDateTime::compareTo).orElse(null);
                if (promisedHours > 0 && harvestTime != null && inboundTime != null) {
                    ok = Duration.between(harvestTime, inboundTime).toHours() <= promisedHours;
                }
            } else if ("HARVEST_DATE".equalsIgnoreCase(type) && batch != null
                    && batch.getExpectHarvestDate() != null && batch.getActualHarvestDate() != null) {
                long diff = Math.abs(java.time.temporal.ChronoUnit.DAYS.between(
                        batch.getExpectHarvestDate(), batch.getActualHarvestDate()));
                ok = diff <= 3;
            } else if ("QUALITY".equalsIgnoreCase(type)) {
                ok = reportMapper.selectByBatchId(batchId).stream()
                        .anyMatch(r -> "合格".equals(r.getResult()));
            }
            if (ok) fulfilled++;
        }
        double ratio = (double) fulfilled / promises.size();
        return (int) Math.round(ratio * 10);
    }

    /** 形如 "24h" / "12H" / "24" → 24；解析失败返回 0 */
    private int parseHours(String value) {
        if (value == null) return 0;
        try {
            String s = value.trim().replaceAll("[Hh小时]", "").trim();
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
