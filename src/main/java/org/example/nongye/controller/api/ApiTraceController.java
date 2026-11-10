package org.example.nongye.controller.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.example.nongye.common.ApiException;
import org.example.nongye.common.ApiResult;
import org.example.nongye.entity.*;
import org.example.nongye.mapper.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 消费者端公开 REST 接口（无需登录）：扫码溯源详情 + 投诉
 */
@RestController
@RequestMapping("/api")
public class ApiTraceController {

    private static final Logger log = LoggerFactory.getLogger(ApiTraceController.class);

    @Autowired
    private CropBatchMapper batchMapper;
    @Autowired
    private FarmingLogMapper farmingLogMapper;
    @Autowired
    private TraceRecordMapper traceRecordMapper;
    @Autowired
    private DetectionReportMapper reportMapper;
    @Autowired
    private ComplaintMapper complaintMapper;
    @Autowired
    private ScanLogMapper scanLogMapper;
    @Autowired
    private org.example.nongye.service.TrustScoreService trustScoreService;
    @Autowired
    private org.example.nongye.service.TrustPromiseService trustPromiseService;
    @Autowired
    private ConsumerFollowMapper consumerFollowMapper;
    @Autowired
    private SysUserMapper userMapper;

    /** 扫码溯源详情（公开）：记录扫码日志，供扫码统计 */
    @GetMapping("/trace/{traceCode}")
    public ApiResult<Map<String, Object>> traceDetail(@PathVariable String traceCode,
                                                      HttpServletRequest request, HttpSession session) {
        CropBatch batch = batchMapper.selectByTraceCode(traceCode);
        if (batch == null) {
            throw ApiException.badRequest("溯源码无效或不存在");
        }
        // 记录扫码日志
        try {
            ScanLog scanLog = new ScanLog();
            scanLog.setBatchId(batch.getId());
            scanLog.setTraceCode(traceCode);
            scanLog.setIp(getClientIp(request));
            scanLog.setUserAgent(request.getHeader("User-Agent"));
            scanLog.setScene("PACKAGE");
            Long consumerId = (Long) session.getAttribute("consumerId");
            if (consumerId != null) {
                scanLog.setConsumerId(consumerId);
            }
            scanLogMapper.insert(scanLog);
        } catch (Exception e) {
            log.warn("扫码日志记录失败", e);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("batch", batch);
        List<FarmingLog> logs = farmingLogMapper.selectByBatchId(batch.getId());
        List<TraceRecord> records = traceRecordMapper.selectByBatchId(batch.getId());
        List<DetectionReport> reports = reportMapper.selectByBatchId(batch.getId());
        data.put("logs", logs);
        data.put("records", records);
        data.put("reports", reports);
        // 可信徽章：流转节点数
        data.put("nodeCount", traceRecordMapper.countByBatchId(batch.getId()));
        // AI 可信指数（确定性规则引擎评分，前端展示红黄绿仪表与逐项依据）
        Map<String, Object> trust = trustScoreService.evaluate(batch.getId());
        data.put("trust", trust);

        // 信任关系：农户档案 + 本人是否已关注/已加入常购
        data.put("farmer", buildFarmer(batch));
        data.put("following", buildFollowing(session, batch));

        // 承诺兑现：农户事前承诺 vs 实际流转数据
        data.put("promise", trustPromiseService.fulfillment(batch.getId()));

        // 决策翻译：把检测/农事/流转数据翻译成"这菜该不该买、怎么存怎么吃"
        data.put("decision", buildDecision(batch, trust, reports, records));
        return ApiResult.ok(data);
    }

    /** 农户档案（公开信息，手机号脱敏） */
    private Map<String, Object> buildFarmer(CropBatch batch) {
        Map<String, Object> farmer = new HashMap<>();
        SysUser u = batch.getFarmerId() == null ? null : userMapper.selectById(batch.getFarmerId());
        farmer.put("id", batch.getFarmerId());
        farmer.put("name", u != null ? (u.getRealName() != null ? u.getRealName() : u.getUsername()) : (batch.getFarmerName() != null ? batch.getFarmerName() : "生态农户"));
        farmer.put("location", batch.getLandLocation());
        farmer.put("phoneMasked", maskPhone(u != null ? u.getPhone() : batch.getFarmerPhone()));
        return farmer;
    }

    /** 当前消费者对该批次/农户的关注与常购状态 */
    private Map<String, Object> buildFollowing(HttpSession session, CropBatch batch) {
        Map<String, Object> following = new HashMap<>();
        Object cidObj = session.getAttribute("consumerId");
        following.put("registered", cidObj != null);
        following.put("farmerFollowed", false);
        following.put("batchFollowed", false);
        if (cidObj == null) return following;
        Long consumerId = ((Number) cidObj).longValue();
        if (batch.getFarmerId() != null) {
            following.put("farmerFollowed",
                    consumerFollowMapper.selectByConsumerAndTarget(consumerId, "FARMER", batch.getFarmerId()) != null);
        }
        following.put("batchFollowed",
                consumerFollowMapper.selectByConsumerAndTarget(consumerId, "BATCH", batch.getId()) != null);
        return following;
    }

    /**
     * 决策翻译（确定性规则，非大模型）：把可信指数与关键事实翻译成购买建议 +
     * 风险提示 + 保存/食用建议，避免消费者"看不懂报告"。
     */
    private Map<String, Object> buildDecision(CropBatch batch, Map<String, Object> trust,
                                              List<DetectionReport> reports, List<TraceRecord> records) {
        int score = trust.get("score") == null ? 0 : ((Number) trust.get("score")).intValue();
        String level = String.valueOf(trust.getOrDefault("level", "YELLOW"));
        boolean hasQualified = reports.stream().anyMatch(r -> "合格".equals(r.getResult()));
        boolean hasReport = !reports.isEmpty();

        List<String> reasons = new ArrayList<>();
        List<String> cautions = new ArrayList<>();

        if (hasQualified) {
            DetectionReport q = reports.stream().filter(r -> "合格".equals(r.getResult())).findFirst().get();
            reasons.add("第三方检测合格（" + (q.getInstitution() == null ? "检测机构" : q.getInstitution()) + "）");
        } else if (hasReport) {
            cautions.add("检测结果为不合格，请谨慎购买");
        } else {
            cautions.add("该批次暂未上传检测报告，建议向商家索取");
        }

        long daysSinceHarvest = -1;
        if (batch.getActualHarvestDate() != null) {
            daysSinceHarvest = java.time.temporal.ChronoUnit.DAYS.between(batch.getActualHarvestDate(), java.time.LocalDate.now());
            if (daysSinceHarvest <= 2) reasons.add("采收后 " + Math.max(daysSinceHarvest, 0) + " 天，正值新鲜期");
            else if (daysSinceHarvest <= 7) reasons.add("采收已 " + daysSinceHarvest + " 天，仍在新保鲜期内");
            else cautions.add("采收已 " + daysSinceHarvest + " 天，注意检查品相");
        }
        long nodeCount = records.size();
        if (nodeCount >= 3) reasons.add("流转链路完整（" + nodeCount + " 个节点可追溯）");
        else cautions.add("流转记录仅 " + nodeCount + " 条，来源链条待补全");

        String verdict;
        String advice;
        if (score >= 80) {
            verdict = "建议购买";
            advice = "数据完整、检测合格，可放心买回家";
        } else if (score >= 60) {
            verdict = "可以买，注意核对";
            advice = "整体可信，个别环节数据待补全，收货时留意品相";
        } else {
            verdict = "建议谨慎";
            advice = "关键数据缺失或存在风险项，建议先向商家确认";
        }

        String crop = batch.getCropName() == null ? "蔬菜" : batch.getCropName();
        Map<String, String> shelf = new HashMap<>();
        shelf.put("叶菜类", "用厨房纸包裹后装保鲜袋，冷藏 2-3 天，避免水洗后存放");
        shelf.put("番茄", "常温阴凉处 3-5 天，熟透后再冷藏，冷藏易失风味");
        shelf.put("黄瓜", "保鲜袋包好冷藏 3-5 天，避免与番茄同放");
        shelf.put("水果", "常温催熟、冷藏保鲜，切开后密封冷藏 1-2 天");
        String shelfTip = shelf.getOrDefault(crop.contains("番茄") ? "番茄" : crop,
                "阴凉通风处存放，尽快食用；冷藏前不要水洗");

        Map<String, Object> decision = new HashMap<>();
        decision.put("verdict", verdict);
        decision.put("advice", advice);
        decision.put("level", level);
        decision.put("reasons", reasons);
        decision.put("cautions", cautions);
        decision.put("shelfTip", shelfTip);
        decision.put("daysSinceHarvest", daysSinceHarvest);
        return decision;
    }

    /** 消费者提交投诉（公开）：投诉人和内容必填 */
    @PostMapping("/complaint")
    public ApiResult<Map<String, Object>> submitComplaint(@RequestBody Complaint complaint, HttpSession session) {
        if (isBlank(complaint.getComplainant())) {
            throw ApiException.badRequest("投诉人不能为空");
        }
        if (isBlank(complaint.getContent())) {
            throw ApiException.badRequest("投诉内容不能为空");
        }
        if (complaint.getBatchId() != null && batchMapper.selectById(complaint.getBatchId()) == null) {
            throw ApiException.badRequest("关联批次不存在");
        }
        Long consumerId = (Long) session.getAttribute("consumerId");
        if (consumerId != null) {
            complaint.setConsumerId(consumerId);
        }
        complaint.setSlaHours(24);
        // 生成查询码：消费者凭此查询进度/确认办结，避免用自增 ID 枚举他人投诉
        complaint.setQueryCode(generateQueryCode());
        complaintMapper.insert(complaint);
        log.info("收到投诉：{} 批次ID：{} 查询码：{}",
                complaint.getComplainant(), complaint.getBatchId(), complaint.getQueryCode());
        Map<String, Object> data = new HashMap<>();
        data.put("id", complaint.getId());
        data.put("queryCode", complaint.getQueryCode());
        if (complaint.getBatchId() != null) {
            CropBatch batch = batchMapper.selectById(complaint.getBatchId());
            if (batch != null && batch.getTraceCode() != null) {
                data.put("traceCode", batch.getTraceCode());
            }
        }
        return ApiResult.ok(data);
    }

    /** 消费者按查询码或手机号查询投诉进度（公开，手机号做脱敏返回） */
    @GetMapping("/complaint/query")
    public ApiResult<List<Complaint>> complaintQuery(@RequestParam(required = false) String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return ApiResult.ok(List.of());
        }
        List<Complaint> list = complaintMapper.selectByPhoneOrCode(keyword.trim());
        // 脱敏：查询结果不回显完整手机号
        for (Complaint c : list) {
            c.setPhone(maskPhone(c.getPhone()));
        }
        return ApiResult.ok(list);
    }

    /** 消费者确认回复：需同时校验查询码，仅"处理中"状态可确认，提交后直接关闭工单 */
    @PostMapping("/complaint/{id}/confirm")
    public ApiResult<Void> confirmComplaint(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String consumerReply = body.getOrDefault("consumerReply", "").trim();
        String queryCode = body.getOrDefault("queryCode", "").trim();
        if (consumerReply.isEmpty()) {
            throw ApiException.badRequest("请填写确认内容");
        }
        if (queryCode.isEmpty()) {
            throw ApiException.badRequest("缺少查询码，无法确认");
        }
        Complaint c = complaintMapper.selectById(id);
        if (c == null) {
            throw ApiException.badRequest("投诉不存在");
        }
        if (complaintMapper.countByIdAndQueryCode(id, queryCode) == 0) {
            throw ApiException.badRequest("查询码不正确");
        }
        if (!"PROCESSING".equals(c.getStatus())) {
            throw ApiException.badRequest("该工单当前状态不支持确认");
        }
        complaintMapper.consumerReply(id, consumerReply);
        log.info("消费者确认投诉[{}]，工单已关闭", id);
        return ApiResult.ok();
    }

    /** 8 位查询码（去掉易混淆的 0/O/1/I） */
    private String generateQueryCode() {
        final String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        java.security.SecureRandom random = new java.security.SecureRandom();
        StringBuilder sb = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    /** 手机号脱敏：13812345678 → 138****5678 */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone == null ? null : "***";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip != null && ip.length() > 50 ? ip.substring(0, 50) : ip;
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
