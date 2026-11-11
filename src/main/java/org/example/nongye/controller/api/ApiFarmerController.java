package org.example.nongye.controller.api;

import jakarta.servlet.http.HttpSession;
import org.example.nongye.common.ApiException;
import org.example.nongye.common.ApiResult;
import org.example.nongye.entity.Complaint;
import org.example.nongye.entity.CropBatch;
import org.example.nongye.entity.DetectionReport;
import org.example.nongye.entity.FarmingLog;
import org.example.nongye.entity.SysUser;
import org.example.nongye.entity.TraceRecord;
import org.example.nongye.entity.TrustPromise;
import org.example.nongye.mapper.ComplaintMapper;
import org.example.nongye.mapper.CropBatchMapper;
import org.example.nongye.mapper.DetectionReportMapper;
import org.example.nongye.mapper.FarmerKpiMapper;
import org.example.nongye.mapper.FarmingLogMapper;
import org.example.nongye.mapper.TraceRecordMapper;
import org.example.nongye.mapper.TrustPromiseMapper;
import org.example.nongye.service.TrustPromiseService;
import org.example.nongye.util.LanIpUtil;
import org.example.nongye.util.QRCodeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 农户端 REST 接口
 */
@RestController
@RequestMapping("/api/farmer")
public class ApiFarmerController {

    private static final Logger log = LoggerFactory.getLogger(ApiFarmerController.class);

    @Autowired
    private CropBatchMapper batchMapper;
    @Autowired
    private FarmingLogMapper farmingLogMapper;
    @Autowired
    private TraceRecordMapper traceRecordMapper;
    @Autowired
    private DetectionReportMapper reportMapper;
    @Autowired
    private TrustPromiseMapper promiseMapper;
    @Autowired
    private TrustPromiseService trustPromiseService;
    @Autowired
    private FarmerKpiMapper kpiMapper;
    @Autowired
    private ComplaintMapper complaintMapper;

    @Value("${upload.path}")
    private String uploadPath;

    @Value("${app.base-url:http://localhost:8080}")
    private String appBaseUrl;

    @Value("${server.port:8080}")
    private int serverPort;

    private SysUser current(HttpSession session) {
        SysUser user = (SysUser) session.getAttribute("loginUser");
        if (user == null) {
            throw ApiException.unauthorized("请先登录");
        }
        if (!"farmer".equals(user.getRole())) {
            throw ApiException.forbidden("无权限");
        }
        return user;
    }

    /** 校验批次归属当前农户 */
    private CropBatch ownBatch(Long id, SysUser user) {
        CropBatch batch = batchMapper.selectById(id);
        if (batch == null || !batch.getFarmerId().equals(user.getId())) {
            throw ApiException.forbidden("无权限");
        }
        return batch;
    }

    @GetMapping("/dashboard")
    public ApiResult<Map<String, Object>> dashboard(HttpSession session) {
        SysUser user = current(session);
        List<CropBatch> batches = batchMapper.selectByFarmerId(user.getId());

        // 按作物汇总采收重量（饼图）
        Map<String, BigDecimal> cropStats = new LinkedHashMap<>();
        for (CropBatch b : batches) {
            if (b.getHarvestWeight() != null) {
                cropStats.merge(b.getCropName(), b.getHarvestWeight(), BigDecimal::add);
            }
        }
        List<DetectionReport> reports = reportMapper.selectByFarmer(user.getId());

        Map<String, Object> data = new HashMap<>();
        data.put("plantingCount", batches.stream().filter(b -> "PLANTING".equals(b.getStatus())).count());
        data.put("harvestedCount", batches.stream().filter(b -> "HARVESTED".equals(b.getStatus()) || "SOLD".equals(b.getStatus())).count());
        data.put("totalBatches", batches.size());
        data.put("reportCount", reports.size());
        data.put("cropStats", cropStats.entrySet().stream()
                .map(e -> Map.of("name", e.getKey(), "value", e.getValue()))
                .toList());
        data.put("recentBatches", batches.stream().limit(5).toList());

        // 信任经营 KPI：信任者数 / 复购率 / 售后闭环时长
        data.put("followers", kpiMapper.countFollowers(user.getId()));
        Map<String, Object> rep = kpiMapper.repurchaseStats(user.getId());
        long total = rep == null || rep.get("totalCnt") == null ? 0 : ((Number) rep.get("totalCnt")).longValue();
        long repeat = rep == null || rep.get("repeatCnt") == null ? 0 : ((Number) rep.get("repeatCnt")).longValue();
        data.put("repurchaseRate", total == 0 ? 0 : Math.round(repeat * 100.0 / total));
        data.put("repeatConsumers", repeat);
        data.put("subscribedConsumers", total);
        data.put("avgCloseHours", Math.round(kpiMapper.avgFirstReplyHours(user.getId()) * 10) / 10.0);
        data.put("promiseCount", promiseMapper.selectByFarmer(user.getId()).size());
        return ApiResult.ok(data);
    }

    @GetMapping("/batches")
    public ApiResult<List<CropBatch>> batches(@RequestParam(required = false) String keyword, HttpSession session) {
        SysUser user = current(session);
        return ApiResult.ok(batchMapper.searchByFarmer(user.getId(), keyword));
    }

    @PostMapping("/batches")
    public ApiResult<CropBatch> addBatch(@RequestBody CropBatch batch, HttpSession session) {
        SysUser user = current(session);
        if (isBlank(batch.getCropName())) {
            throw ApiException.badRequest("作物名称不能为空");
        }
        if (batch.getPlantDate() == null) {
            throw ApiException.badRequest("请选择播种日期");
        }
        batch.setFarmerId(user.getId());
        String batchNo = "BATCH" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        batch.setBatchNo(batchNo);
        batchMapper.insert(batch);
        log.info("农户[{}]新增种植批次：{}", user.getUsername(), batchNo);
        return ApiResult.ok(batchMapper.selectById(batch.getId()));
    }

    @GetMapping("/batches/{id}")
    public ApiResult<CropBatch> batchDetail(@PathVariable Long id, HttpSession session) {
        SysUser user = current(session);
        return ApiResult.ok(ownBatch(id, user));
    }

    @GetMapping("/batches/{id}/logs")
    public ApiResult<Map<String, Object>> batchLogs(@PathVariable Long id, HttpSession session) {
        SysUser user = current(session);
        CropBatch batch = ownBatch(id, user);
        Map<String, Object> data = new HashMap<>();
        data.put("batch", batch);
        data.put("logs", farmingLogMapper.selectByBatchId(id));
        return ApiResult.ok(data);
    }

    @PostMapping("/batches/{id}/logs")
    public ApiResult<Void> addLog(@PathVariable Long id, @RequestBody FarmingLog farmingLog, HttpSession session) {
        SysUser user = current(session);
        CropBatch batch = ownBatch(id, user);
        if (isBlank(farmingLog.getLogType())) {
            throw ApiException.badRequest("请选择农事类型");
        }
        if (farmingLog.getLogDate() == null) {
            throw ApiException.badRequest("请选择记录日期");
        }
        farmingLog.setBatchId(id);
        farmingLogMapper.insert(farmingLog);
        log.info("农户[{}]为批次[{}]添加农事记录：{}", user.getUsername(), batch.getBatchNo(), farmingLog.getLogType());
        return ApiResult.ok();
    }

    @GetMapping("/logs")
    public ApiResult<Map<String, Object>> allLogs(HttpSession session) {
        SysUser user = current(session);
        Map<String, Object> data = new HashMap<>();
        data.put("logs", farmingLogMapper.selectByFarmer(user.getId()));
        data.put("batches", batchMapper.selectByFarmerId(user.getId()));
        return ApiResult.ok(data);
    }

    @PostMapping("/batches/{id}/harvest")
    @Transactional(rollbackFor = Exception.class)
    public ApiResult<CropBatch> harvest(@PathVariable Long id, @RequestBody Map<String, Object> body,
                                        HttpSession session) throws Exception {
        SysUser user = current(session);
        CropBatch batch = ownBatch(id, user);
        if (!"PLANTING".equals(batch.getStatus())) {
            throw ApiException.badRequest("当前批次状态不允许采收");
        }
        String harvestDate = body.getOrDefault("actualHarvestDate", "").toString();
        BigDecimal weight;
        try {
            weight = new BigDecimal(body.getOrDefault("harvestWeight", "0").toString());
        } catch (NumberFormatException e) {
            throw ApiException.badRequest("采收重量格式不正确");
        }
        if (harvestDate.isBlank() || weight.compareTo(BigDecimal.ZERO) <= 0) {
            throw ApiException.badRequest("请填写采收日期和大于 0 的采收重量");
        }

        String traceCode = "TZ" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        batch.setTraceCode(traceCode);
        batch.setActualHarvestDate(LocalDate.parse(harvestDate));
        batch.setHarvestWeight(weight);

        File qrDir = new File(uploadPath, "qrcode").getAbsoluteFile();
        qrDir.mkdirs();
        String qrFileName = "qr_" + traceCode + ".png";
        // 动态探测局域网 IP：换 WiFi / 热点后生成的二维码依然可扫
        String qrBaseUrl = LanIpUtil.getLanUrl(serverPort, appBaseUrl);
        QRCodeUtil.generateQRCode(qrBaseUrl + "/trace/" + traceCode,
                new File(qrDir, qrFileName).getAbsolutePath(), 300, 300);
        batch.setQrCodePath("/uploads/qrcode/" + qrFileName);

        batchMapper.harvest(batch);
        log.info("批次[{}]采收完成，溯源码：{}", batch.getBatchNo(), traceCode);

        TraceRecord record = new TraceRecord();
        record.setBatchId(id);
        record.setFromUser(user.getId());
        record.setType("HARVEST");
        record.setQuantity(weight);
        record.setLocation(batch.getLandLocation());
        record.setRemark("农户采收");
        traceRecordMapper.insert(record);

        return ApiResult.ok(batchMapper.selectById(id));
    }

    @GetMapping("/trace-codes")
    public ApiResult<Map<String, Object>> traceCodes(HttpSession session) {
        SysUser user = current(session);
        Map<String, Object> data = new HashMap<>();
        data.put("batches", batchMapper.selectByFarmerId(user.getId()));
        // 动态显示当前局域网地址，换网络自动更新
        data.put("baseUrl", LanIpUtil.getLanUrl(serverPort, appBaseUrl));
        return ApiResult.ok(data);
    }

    @GetMapping("/reports")
    public ApiResult<Map<String, Object>> reports(HttpSession session) {
        SysUser user = current(session);
        Map<String, Object> data = new HashMap<>();
        data.put("reports", reportMapper.selectByFarmer(user.getId()));
        data.put("batches", batchMapper.selectByFarmerId(user.getId()));
        return ApiResult.ok(data);
    }

    @PostMapping("/reports")
    public ApiResult<Void> uploadReport(@RequestParam Long batchId,
                                        @RequestParam(required = false) String reportNo,
                                        @RequestParam(required = false) String institution,
                                        @RequestParam(required = false) String reportDate,
                                        @RequestParam String result,
                                        @RequestParam(required = false) MultipartFile file,
                                        HttpSession session) throws Exception {
        SysUser user = current(session);
        ownBatch(batchId, user);

        DetectionReport report = new DetectionReport();
        report.setBatchId(batchId);
        report.setReportNo(trimOrNull(reportNo));
        report.setInstitution(trimOrNull(institution));
        report.setResult(result);
        if (reportDate != null && !reportDate.isEmpty()) {
            report.setReportDate(LocalDate.parse(reportDate));
        }

        // 保存检测报告文件（PDF/图片）
        if (file != null && !file.isEmpty()) {
            String original = file.getOriginalFilename();
            String ext = original != null && original.contains(".")
                    ? original.substring(original.lastIndexOf('.'))
                    : "";
            if (!ext.toLowerCase().matches("\\.(pdf|png|jpg|jpeg)")) {
                throw ApiException.badRequest("仅支持 PDF/PNG/JPG 文件");
            }
            File dir = new File(uploadPath, "reports").getAbsoluteFile();
            dir.mkdirs();
            String fileName = UUID.randomUUID().toString().replace("-", "") + ext;
            // 必须用绝对路径：MultipartFile.transferTo 的相对路径会被解析到 Tomcat 临时目录
            file.transferTo(new File(dir, fileName).getAbsoluteFile());
            report.setReportUrl("/uploads/reports/" + fileName);
        }

        reportMapper.insert(report);
        log.info("农户[{}]上传批次[{}]检测报告，结果：{}", user.getUsername(), batchId, result);
        return ApiResult.ok();
    }

    @GetMapping("/sales")
    public ApiResult<List<TraceRecord>> sales(HttpSession session) {
        SysUser user = current(session);
        return ApiResult.ok(traceRecordMapper.selectSalesByFarmer(user.getId()));
    }

    // ==================== 信任承诺（事前公开承诺 + 事后兑现） ====================

    /** 承诺类型字典 + 本农户全部承诺 */
    @GetMapping("/promises")
    public ApiResult<Map<String, Object>> promises(HttpSession session) {
        SysUser user = current(session);
        Map<String, Object> data = new HashMap<>();
        data.put("promises", promiseMapper.selectByFarmer(user.getId()));
        data.put("batches", batchMapper.selectByFarmerId(user.getId()));
        data.put("types", List.of(
                Map.of("type", "FRESH", "label", "新鲜承诺", "hint", "如 24h，表示采收后 24 小时内送达经销商"),
                Map.of("type", "HARVEST_DATE", "label", "采收期承诺", "hint", "如 约定采收日，前后 3 天内视为兑现"),
                Map.of("type", "QUALITY", "label", "品质承诺", "hint", "如 无农残、不打蜡，可附检测报告")
        ));
        return ApiResult.ok(data);
    }

    /** 新增承诺：只能给自己名下的批次设置 */
    @PostMapping("/promises")
    public ApiResult<TrustPromise> createPromise(@RequestBody Map<String, Object> body, HttpSession session) {
        SysUser user = current(session);
        Long batchId = body.get("batchId") == null ? null
                : Long.valueOf(body.get("batchId").toString());
        String type = body.get("promiseType") == null ? "" : body.get("promiseType").toString().trim();
        String value = body.get("promiseValue") == null ? "" : body.get("promiseValue").toString().trim();
        String evidence = body.get("evidencePath") == null ? null : body.get("evidencePath").toString().trim();
        if (batchId == null) throw ApiException.badRequest("请选择批次");
        if (type.isEmpty()) throw ApiException.badRequest("请选择承诺类型");
        if (value.isEmpty()) throw ApiException.badRequest("请填写承诺内容");
        ownBatch(batchId, user);
        TrustPromise p = trustPromiseService.create(batchId, user.getId(), type, value, evidence);
        log.info("农户[{}]为批次[{}]新增承诺：{}={}", user.getUsername(), batchId, type, value);
        return ApiResult.ok(p);
    }

    @DeleteMapping("/promises/{id}")
    public ApiResult<Void> deletePromise(@PathVariable Long id, HttpSession session) {
        SysUser user = current(session);
        TrustPromise p = promiseMapper.selectById(id);
        if (p == null) throw ApiException.badRequest("承诺不存在");
        if (p.getFarmerId() != null && !p.getFarmerId().equals(user.getId())) {
            throw ApiException.forbidden("无权删除他人承诺");
        }
        promiseMapper.deleteById(id);
        return ApiResult.ok();
    }

    // ==================== 售后工单（本农户批次收到的投诉） ====================

    @GetMapping("/complaints")
    public ApiResult<Map<String, Object>> complaints(HttpSession session) {
        SysUser user = current(session);
        List<Complaint> list = complaintMapper.selectByFarmer(user.getId());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("complaints", list);
        data.put("pendingCount", list.stream().filter(c -> "PENDING".equals(c.getStatus())).count());
        data.put("overdueCount", list.stream().filter(c -> Integer.valueOf(1).equals(c.getOverdue())).count());
        return ApiResult.ok(data);
    }

    /** 农户回复工单：只能处理自己批次上的投诉，回复后状态置为处理中 */
    @PostMapping("/complaints/{id}/reply")
    public ApiResult<Void> replyComplaint(@PathVariable Long id, @RequestBody Map<String, String> body, HttpSession session) {
        SysUser user = current(session);
        Complaint c = complaintMapper.selectById(id);
        if (c == null) throw ApiException.badRequest("工单不存在");
        if (c.getBatchId() != null) ownBatch(c.getBatchId(), user);
        if ("CLOSED".equals(c.getStatus())) throw ApiException.badRequest("工单已关闭，无法回复");
        String reply = body.get("reply");
        if (isBlank(reply)) throw ApiException.badRequest("请填写处理说明");
        Complaint update = new Complaint();
        update.setId(id);
        update.setReply(reply.trim());
        update.setStatus("PROCESSING");
        complaintMapper.reply(update);
        log.info("农户[{}]回复工单[{}]：{}", user.getUsername(), id, reply.trim());
        return ApiResult.ok();
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private String trimOrNull(String s) {
        return s == null || s.trim().isEmpty() ? null : s.trim();
    }
}
