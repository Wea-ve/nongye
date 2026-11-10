package org.example.nongye.controller.api;

import jakarta.servlet.http.HttpSession;
import org.example.nongye.common.ApiException;
import org.example.nongye.common.ApiResult;
import org.example.nongye.entity.CropBatch;
import org.example.nongye.entity.DetectionReport;
import org.example.nongye.entity.FarmingLog;
import org.example.nongye.entity.SysUser;
import org.example.nongye.entity.TraceRecord;
import org.example.nongye.mapper.CropBatchMapper;
import org.example.nongye.mapper.DetectionReportMapper;
import org.example.nongye.mapper.FarmingLogMapper;
import org.example.nongye.mapper.TraceRecordMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 经销商端 REST 接口
 */
@RestController
@RequestMapping("/api/dealer")
public class ApiDealerController {

    private static final Logger log = LoggerFactory.getLogger(ApiDealerController.class);

    @Autowired
    private CropBatchMapper batchMapper;
    @Autowired
    private TraceRecordMapper traceRecordMapper;
    @Autowired
    private FarmingLogMapper farmingLogMapper;
    @Autowired
    private DetectionReportMapper reportMapper;

    private SysUser current(HttpSession session) {
        SysUser user = (SysUser) session.getAttribute("loginUser");
        if (user == null) {
            throw ApiException.unauthorized("请先登录");
        }
        if (!"dealer".equals(user.getRole())) {
            throw ApiException.forbidden("无权限");
        }
        return user;
    }

    @GetMapping("/dashboard")
    public ApiResult<Map<String, Object>> dashboard(HttpSession session) {
        SysUser user = current(session);
        List<CropBatch> available = batchMapper.selectAll().stream()
                .filter(b -> "HARVESTED".equals(b.getStatus()))
                .collect(Collectors.toList());
        List<CropBatch> involved = batchMapper.selectByDealerInvolved(user.getId());
        List<TraceRecord> inbound = traceRecordMapper.selectByUserAndType(user.getId(), "INBOUND");
        List<TraceRecord> outbound = traceRecordMapper.selectByUserAndType(user.getId(), "OUTBOUND");

        Map<String, Object> data = new HashMap<>();
        data.put("realName", user.getRealName());
        data.put("availableCount", available.size());
        data.put("inboundCount", inbound.size());
        data.put("outboundCount", outbound.size());
        data.put("involvedCount", involved.size());
        data.put("available", available);
        data.put("recentRecords", traceRecordMapper.selectByUserAndType(user.getId(), "INBOUND")
                .stream().limit(5).collect(Collectors.toList()));

        // 周转约束：已入库超过 48h 仍未出库的批次（提醒经销商别压货）
        java.util.Set<Long> outboundBatchIds = outbound.stream()
                .map(TraceRecord::getBatchId).collect(Collectors.toSet());
        java.time.LocalDateTime deadline = java.time.LocalDateTime.now().minusHours(48);
        long staleOutbound = inbound.stream()
                .filter(r -> r.getBatchId() != null && !outboundBatchIds.contains(r.getBatchId()))
                .filter(r -> r.getOperateTime() != null && r.getOperateTime().isBefore(deadline))
                .map(TraceRecord::getBatchId).distinct().count();
        data.put("staleOutbound", staleOutbound);
        return ApiResult.ok(data);
    }

    /** 可入库批次（已采收未入库） */
    @GetMapping("/batches/available")
    public ApiResult<List<CropBatch>> available(HttpSession session) {
        current(session);
        List<CropBatch> all = batchMapper.selectAll();
        return ApiResult.ok(all.stream()
                .filter(b -> "HARVESTED".equals(b.getStatus()))
                .collect(Collectors.toList()));
    }

    @PostMapping("/inbound")
    @Transactional(rollbackFor = Exception.class)
    public ApiResult<Void> inbound(@RequestBody Map<String, Object> body, HttpSession session) {
        SysUser user = current(session);
        Long batchId = parseLong(body.get("batchId"), "请选择要入库的批次");
        BigDecimal quantity = parseDecimal(body.get("quantity"), "请填写入库数量");
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw ApiException.badRequest("入库数量必须大于 0");
        }
        CropBatch batch = batchMapper.selectById(batchId);
        if (batch == null) {
            throw ApiException.badRequest("批次不存在");
        }
        if (!"HARVESTED".equals(batch.getStatus())) {
            throw ApiException.badRequest("该批次当前状态不允许入库");
        }
        // 防止同一经销商对同一批次重复入库
        List<TraceRecord> existing = traceRecordMapper.selectByUserAndType(user.getId(), "INBOUND");
        boolean alreadyInbound = existing.stream().anyMatch(r -> batchId.equals(r.getBatchId()));
        if (alreadyInbound) {
            throw ApiException.badRequest("该批次您已经入库过，请勿重复操作");
        }
        // 数量上限：全平台入库总量不得超过该批次采收量
        BigDecimal inboundTotal = traceRecordMapper.sumQuantityByBatchAndType(batchId, "INBOUND");
        if (batch.getHarvestWeight() != null) {
            BigDecimal remain = batch.getHarvestWeight().subtract(inboundTotal);
            if (quantity.compareTo(remain) > 0) {
                throw ApiException.badRequest("入库数量超出该批次可入库余量，当前最多可入库 "
                        + remain.max(BigDecimal.ZERO).stripTrailingZeros().toPlainString() + " 斤");
            }
        }
        TraceRecord record = new TraceRecord();
        record.setBatchId(batchId);
        record.setFromUser(batch.getFarmerId());
        record.setToUser(user.getId());
        record.setType("INBOUND");
        record.setQuantity(quantity);
        record.setLocation(user.getAddress());
        record.setRemark("经销商收货入库");
        traceRecordMapper.insert(record);
        log.info("经销商[{}]入库批次[{}] 数量：{} 斤", user.getUsername(), batch.getBatchNo(), quantity);
        return ApiResult.ok();
    }

    /** 发货出库页：经手批次列表 + 已入库批次 ID 集合（前端据此判断能否出库） */
    @GetMapping("/outbound-data")
    public ApiResult<Map<String, Object>> outboundData(HttpSession session) {
        SysUser user = current(session);
        List<CropBatch> list = batchMapper.selectByDealerInvolved(user.getId());
        Set<Long> inboundIds = traceRecordMapper.selectByUserAndType(user.getId(), "INBOUND").stream()
                .map(TraceRecord::getBatchId)
                .collect(Collectors.toSet());
        Map<String, Object> data = new HashMap<>();
        data.put("list", list);
        data.put("inboundIds", inboundIds);
        return ApiResult.ok(data);
    }

    @PostMapping("/outbound")
    @Transactional(rollbackFor = Exception.class)
    public ApiResult<Void> outbound(@RequestBody Map<String, Object> body, HttpSession session) {
        SysUser user = current(session);
        Long batchId = parseLong(body.get("batchId"), "请选择要出库的批次");
        BigDecimal quantity = parseDecimal(body.get("quantity"), "请填写出库数量");
        String toName = body.getOrDefault("toName", "").toString().trim();
        String location = body.getOrDefault("location", "").toString().trim();
        if (toName.isEmpty() || location.isEmpty()) {
            throw ApiException.badRequest("请填写收货方和发货地点");
        }
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw ApiException.badRequest("出库数量必须大于 0");
        }
        CropBatch batch = batchMapper.selectById(batchId);
        if (batch == null) {
            throw ApiException.badRequest("批次不存在");
        }
        // 校验该经销商必须先入库过才能出库
        List<TraceRecord> inboundRecords = traceRecordMapper.selectByUserAndType(user.getId(), "INBOUND");
        boolean hasInbound = inboundRecords.stream().anyMatch(r -> batchId.equals(r.getBatchId()));
        if (!hasInbound) {
            throw ApiException.badRequest("该批次尚未入库，不能出库");
        }
        // 数量上限：出库量不得超过该经销商在本批次的剩余库存（入库量 - 已出库量）
        BigDecimal inQty = traceRecordMapper.sumInboundByDealer(batchId, user.getId());
        BigDecimal outQty = traceRecordMapper.sumOutboundByDealer(batchId, user.getId());
        BigDecimal stock = inQty.subtract(outQty);
        if (quantity.compareTo(stock) > 0) {
            throw ApiException.badRequest("出库数量超出剩余库存，当前最多可出库 "
                    + stock.max(BigDecimal.ZERO).stripTrailingZeros().toPlainString() + " 斤");
        }
        TraceRecord record = new TraceRecord();
        record.setBatchId(batchId);
        record.setFromUser(user.getId());
        record.setType("OUTBOUND");
        record.setQuantity(quantity);
        record.setLocation(location);
        record.setRemark("发往：" + toName);
        traceRecordMapper.insert(record);
        // 全平台出库总量 ≥ 入库总量时，批次售罄
        BigDecimal totalIn = traceRecordMapper.sumQuantityByBatchAndType(batchId, "INBOUND");
        BigDecimal totalOut = traceRecordMapper.sumQuantityByBatchAndType(batchId, "OUTBOUND");
        if (totalIn.compareTo(BigDecimal.ZERO) > 0 && totalOut.compareTo(totalIn) >= 0
                && !"SOLD".equals(batch.getStatus())) {
            batchMapper.markSold(batchId);
            log.info("批次[{}]已全部出库，状态更新为 SOLD", batch.getBatchNo());
        }
        log.info("经销商[{}]出库批次[{}] 发往：{} 数量：{} 斤", user.getUsername(), batch.getBatchNo(), toName, quantity);
        return ApiResult.ok();
    }

    @GetMapping("/records")
    public ApiResult<List<CropBatch>> records(@RequestParam(required = false) String keyword, HttpSession session) {
        SysUser user = current(session);
        return ApiResult.ok(batchMapper.searchByDealerInvolved(user.getId(), keyword));
    }

    @GetMapping("/records/inbound")
    public ApiResult<List<TraceRecord>> inboundRecords(HttpSession session) {
        SysUser user = current(session);
        return ApiResult.ok(traceRecordMapper.selectByUserAndType(user.getId(), "INBOUND"));
    }

    @GetMapping("/records/outbound")
    public ApiResult<List<TraceRecord>> outboundRecords(HttpSession session) {
        SysUser user = current(session);
        return ApiResult.ok(traceRecordMapper.selectByUserAndType(user.getId(), "OUTBOUND"));
    }

    /** 经销商溯源查询：不记录扫码日志（区别于消费者扫码） */
    @GetMapping("/trace")
    public ApiResult<Map<String, Object>> traceQuery(@RequestParam String traceCode, HttpSession session) {
        current(session);
        if (traceCode == null || traceCode.trim().isEmpty()) {
            throw ApiException.badRequest("请输入溯源码");
        }
        CropBatch batch = batchMapper.selectByTraceCode(traceCode.trim());
        Map<String, Object> data = new HashMap<>();
        if (batch != null) {
            List<FarmingLog> logs = farmingLogMapper.selectByBatchId(batch.getId());
            List<DetectionReport> reports = reportMapper.selectByBatchId(batch.getId());
            data.put("batch", batch);
            data.put("logs", logs);
            data.put("records", traceRecordMapper.selectByBatchId(batch.getId()));
            data.put("reports", reports);
        }
        return ApiResult.ok(data);
    }

    private Long parseLong(Object v, String errMsg) {
        try {
            return Long.valueOf(v.toString());
        } catch (Exception e) {
            throw ApiException.badRequest(errMsg);
        }
    }

    private BigDecimal parseDecimal(Object v, String errMsg) {
        try {
            return new BigDecimal(v.toString());
        } catch (Exception e) {
            throw ApiException.badRequest(errMsg);
        }
    }
}
