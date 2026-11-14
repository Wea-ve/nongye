package org.example.nongye.controller.api;

import jakarta.servlet.http.HttpSession;
import org.example.nongye.common.ApiException;
import org.example.nongye.common.ApiResult;
import org.example.nongye.common.PasswordHelper;
import org.example.nongye.entity.Complaint;
import org.example.nongye.entity.CropBatch;
import org.example.nongye.entity.SysUser;
import org.example.nongye.mapper.ComplaintMapper;
import org.example.nongye.mapper.CropBatchMapper;
import org.example.nongye.mapper.DetectionReportMapper;
import org.example.nongye.mapper.FarmingLogMapper;
import org.example.nongye.mapper.ScanLogMapper;
import org.example.nongye.mapper.SysUserMapper;
import org.example.nongye.mapper.TraceRecordMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理员端 REST 接口
 */
@RestController
@RequestMapping("/api/admin")
public class ApiAdminController {

    private static final Logger log = LoggerFactory.getLogger(ApiAdminController.class);

    @Autowired
    private SysUserMapper userMapper;
    @Autowired
    private ComplaintMapper complaintMapper;
    @Autowired
    private CropBatchMapper batchMapper;
    @Autowired
    private FarmingLogMapper farmingLogMapper;
    @Autowired
    private DetectionReportMapper reportMapper;
    @Autowired
    private TraceRecordMapper traceRecordMapper;
    @Autowired
    private ScanLogMapper scanLogMapper;
    @Autowired
    private PasswordHelper passwordHelper;

    private SysUser current(HttpSession session) {
        SysUser user = org.example.nongye.common.AuthContext.user();
        if (user == null) {
            user = (SysUser) session.getAttribute("loginUser");
        }
        if (user == null) {
            throw ApiException.unauthorized("请先登录");
        }
        if (!"admin".equals(user.getRole())) {
            throw ApiException.forbidden("无权限");
        }
        return user;
    }

    @GetMapping("/dashboard")
    public ApiResult<Map<String, Object>> dashboard(HttpSession session) {
        current(session);
        List<Complaint> complaints = complaintMapper.selectAll();
        Map<String, Object> data = new HashMap<>();
        data.put("cropStats", batchMapper.countByCrop());
        data.put("complaintTotal", complaints.size());
        data.put("pendingCount", complaints.stream().filter(c -> "PENDING".equals(c.getStatus())).count());
        data.put("batchTotal", batchMapper.selectAll().size());
        data.put("farmerCount", userMapper.selectByRole("farmer").size());
        data.put("dealerCount", userMapper.selectByRole("dealer").size());
        data.put("complaintStatus", complaintMapper.countByStatus());
        // 近 7 天流转量（折线图）
        data.put("trace7days", traceRecordMapper.countByDay(7));
        data.put("scanToday", scanLogMapper.countToday());
        data.put("scan7days", scanLogMapper.countLast7Days());
        return ApiResult.ok(data);
    }

    /** 顶栏铃铛角标：待处理投诉总数 + 已超时升级数 */
    @GetMapping("/pending-count")
    public ApiResult<Map<String, Object>> pendingCount(HttpSession session) {
        current(session);
        Map<String, Object> r = new HashMap<>();
        r.put("count", complaintMapper.countPending());
        r.put("overtime", complaintMapper.countOverduePending());
        return ApiResult.ok(r);
    }

    /**
     * 可视化大屏聚合接口：一次拉齐 KPI、趋势、分布与实时动态
     */
    @GetMapping("/bigscreen")
    public ApiResult<Map<String, Object>> bigscreen(HttpSession session) {
        current(session);
        Map<String, Object> data = new HashMap<>();

        // KPI 指标
        List<CropBatch> batches = batchMapper.selectAll();
        int reportTotal = reportMapper.selectAllWithBatch().size();
        long reportQualified = reportMapper.selectAllWithBatch().stream()
                .filter(r -> "合格".equals(r.getResult())).count();
        data.put("batchTotal", batches.size());
        data.put("farmerCount", userMapper.selectByRole("farmer").size());
        data.put("dealerCount", userMapper.selectByRole("dealer").size());
        data.put("scanToday", scanLogMapper.countToday());
        data.put("scanTotal", scanLogMapper.countAll());
        data.put("pendingCount", complaintMapper.countPending());
        data.put("traceTotal", traceRecordMapper.selectAllWithBatch().size());
        data.put("reportTotal", reportTotal);
        data.put("qualifiedRate", reportTotal == 0 ? 100.0
                : Math.round(reportQualified * 1000.0 / reportTotal) / 10.0);

        // 批次状态分布
        Map<String, Long> statusCount = new HashMap<>();
        batches.forEach(b -> statusCount.merge(b.getStatus(), 1L, Long::sum));
        data.put("batchStatus", statusCount);

        // 趋势与分布
        data.put("scan7days", scanLogMapper.countLast7Days());
        data.put("trace7days", traceRecordMapper.countByDay(7));
        data.put("cropStats", batchMapper.countByCrop());
        data.put("complaintStatus", complaintMapper.countByStatus());
        data.put("hotBatches", scanLogMapper.hotBatches());

        // 实时动态：最新扫码（扫码日志按时间倒序取前 8 条）
        data.put("recentScans", scanLogMapper.selectAll().stream().limit(8).toList());
        return ApiResult.ok(data);
    }

    @GetMapping("/users")
    public ApiResult<Map<String, Object>> users(@RequestParam(required = false) String keyword,
                                                @RequestParam(required = false) String role,
                                                HttpSession session) {
        current(session);
        Map<String, Object> data = new HashMap<>();
        data.put("list", userMapper.searchUsers(keyword, role));
        data.put("allCount", userMapper.selectAllUsers().size());
        data.put("farmerCount", userMapper.selectByRole("farmer").size());
        data.put("dealerCount", userMapper.selectByRole("dealer").size());
        return ApiResult.ok(data);
    }

    @PostMapping("/users")
    public ApiResult<Void> addUser(@RequestBody Map<String, String> body, HttpSession session) {
        current(session);
        String username = body.getOrDefault("username", "").trim();
        String password = body.getOrDefault("password", "");
        String realName = body.getOrDefault("realName", "").trim();
        String role = body.getOrDefault("role", "");
        String phone = body.get("phone");
        String address = body.get("address");

        // 服务端二次校验：角色只能是农户或经销商，禁止通过接口创建管理员
        if (!"farmer".equals(role) && !"dealer".equals(role)) {
            throw ApiException.badRequest("非法的账号角色");
        }
        if (username.length() < 3) {
            throw ApiException.badRequest("账号长度至少 3 位");
        }
        if (password.length() < 6) {
            throw ApiException.badRequest("初始密码长度至少 6 位");
        }
        if (realName.isEmpty()) {
            throw ApiException.badRequest("姓名不能为空");
        }
        if (userMapper.selectByUsername(username) != null) {
            throw ApiException.badRequest("账号已存在，请更换");
        }

        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(passwordHelper.encode(password));
        user.setRealName(realName);
        user.setRole(role);
        user.setPhone(phone == null || phone.trim().isEmpty() ? null : phone.trim());
        user.setAddress(address == null || address.trim().isEmpty() ? null : address.trim());
        userMapper.insert(user);
        log.info("管理员创建新账号：username={}, role={}", username, role);
        return ApiResult.ok();
    }

    @GetMapping("/complaints")
    public ApiResult<List<Complaint>> complaints(@RequestParam(required = false) String keyword, HttpSession session) {
        current(session);
        return ApiResult.ok(complaintMapper.search(keyword));
    }

    /** 回复投诉：按状态机流转 PENDING→PROCESSING→CLOSED */
    @PostMapping("/complaints/{id}/reply")
    public ApiResult<Void> reply(@PathVariable Long id, @RequestBody Map<String, String> body, HttpSession session) {
        SysUser admin = current(session);
        String reply = body.getOrDefault("reply", "").trim();
        String status = body.getOrDefault("status", "");
        if (reply.isEmpty()) {
            throw ApiException.badRequest("回复内容不能为空");
        }
        if (!"PROCESSING".equals(status) && !"CLOSED".equals(status)) {
            throw ApiException.badRequest("非法的目标状态");
        }
        Complaint c = complaintMapper.selectById(id);
        if (c == null) {
            throw ApiException.badRequest("投诉不存在");
        }
        Complaint update = new Complaint();
        update.setId(id);
        update.setReply(reply);
        update.setStatus(status);
        complaintMapper.reply(update);
        log.info("管理员[{}]回复投诉[{}] 状态：{}", admin.getUsername(), id, status);
        return ApiResult.ok();
    }

    @GetMapping("/batches")
    public ApiResult<List<CropBatch>> batches(@RequestParam(required = false) String keyword, HttpSession session) {
        current(session);
        return ApiResult.ok(batchMapper.search(keyword));
    }

    @GetMapping("/farming-logs")
    public ApiResult<?> farmingLogs(HttpSession session) {
        current(session);
        return ApiResult.ok(farmingLogMapper.selectAllWithBatch());
    }

    @GetMapping("/reports")
    public ApiResult<Map<String, Object>> reports(HttpSession session) {
        current(session);
        List<org.example.nongye.entity.DetectionReport> reports = reportMapper.selectAllWithBatch();
        Map<String, Object> data = new HashMap<>();
        data.put("list", reports);
        data.put("qualifiedCount", reports.stream().filter(r -> "合格".equals(r.getResult())).count());
        data.put("unqualifiedCount", reports.stream().filter(r -> "不合格".equals(r.getResult())).count());
        return ApiResult.ok(data);
    }

    @GetMapping("/trace-records")
    public ApiResult<?> traceRecords(@RequestParam(required = false) String keyword, HttpSession session) {
        current(session);
        return ApiResult.ok(traceRecordMapper.search(keyword));
    }

    /** 管理员溯源查询：不记录扫码日志 */
    @GetMapping("/trace")
    public ApiResult<Map<String, Object>> traceQuery(@RequestParam(required = false) String traceCode, HttpSession session) {
        current(session);
        Map<String, Object> data = new HashMap<>();
        if (traceCode != null && !traceCode.trim().isEmpty()) {
            data.put("batch", batchMapper.selectByTraceCode(traceCode.trim()));
        }
        return ApiResult.ok(data);
    }

    @GetMapping("/scan-stats")
    public ApiResult<Map<String, Object>> scanStats(HttpSession session) {
        current(session);
        Map<String, Object> data = new HashMap<>();
        data.put("todayCount", scanLogMapper.countToday());
        data.put("scan7days", scanLogMapper.countLast7Days());
        data.put("hotBatches", scanLogMapper.hotBatches());
        data.put("scanLogs", scanLogMapper.selectAll());
        return ApiResult.ok(data);
    }
}
