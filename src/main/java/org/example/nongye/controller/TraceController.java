package org.example.nongye.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.nongye.entity.*;
import org.example.nongye.mapper.*;
import org.example.nongye.util.LanIpUtil;
import org.example.nongye.util.QRCodeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 消费者端 - 公开访问，无需登录
 */
@Controller
public class TraceController {

    private static final Logger log = LoggerFactory.getLogger(TraceController.class);

    @Value("${app.base-url:http://localhost:8080}")
    private String appBaseUrl;

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

    /**
     * 消费者扫码入口（二维码地址保持 /trace/{code} 不变）：
     * 302 跳转到 Vue SPA 溯源页，扫码日志由 /api/trace/{traceCode} 记录
     */
    @GetMapping("/trace/{traceCode}")
    public String traceDetail(@PathVariable String traceCode) {
        return "redirect:/app/trace/" + traceCode;
    }

    /**
     * SPA 根入口兜底：/app 与 /app/ 统一跳登录页
     * （Spring Boot 4 的 PathPattern 下 /app/** 不匹配目录样请求，资源回退不会触发）
     */
    @GetMapping({"/app", "/app/"})
    public String appIndex() {
        return "redirect:/app/login";
    }

    /**
     * 动态生成二维码：自动使用当前局域网 IP，换 WiFi / 热点无需改代码
     * 公开访问（扫码设备未登录）
     */
    @GetMapping("/qrcode")
    public void qrcode(@RequestParam String code, HttpServletRequest request, HttpServletResponse response) throws Exception {
        int port = request.getServerPort();
        // Docker 部署时容器内探测到的是虚拟网卡 IP（手机无法访问），
        // 显式设置 APP_BASE_URL 环境变量则强制使用该地址
        String envBase = System.getenv("APP_BASE_URL");
        String baseUrl = (envBase != null && !envBase.isBlank())
                ? envBase
                : LanIpUtil.getLanUrl(port, appBaseUrl);
        String content = baseUrl + "/trace/" + code;
        response.setContentType("image/png");
        response.setHeader("Cache-Control", "no-store");
        QRCodeUtil.writeToStream(content, response.getOutputStream(), 220, 220);
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

    /**
     * 消费者独立投诉入口：跳转 Vue SPA 投诉页（旧地址保持兼容）
     */
    @GetMapping("/complaint/new")
    public String complaintForm() {
        return "redirect:/app/complaint/new";
    }

    @PostMapping("/complaint")
    public String submitComplaint(Complaint complaint, Model model) {
        // 必填字段校验，防止空投诉刷库
        if (isBlank(complaint.getComplainant()) || isBlank(complaint.getContent())) {
            return "redirect:/app/complaint/new?error=1";
        }
        complaintMapper.insert(complaint);
        log.info("收到投诉：{} 联系方式：{} 批次ID：{} 查询码：{}",
                complaint.getComplainant(), complaint.getPhone(), complaint.getBatchId(), complaint.getId());
        // 有关联批次则回到溯源页，否则直接跳到查询页展示查询码
        if (complaint.getBatchId() != null) {
            CropBatch batch = batchMapper.selectById(complaint.getBatchId());
            if (batch != null && batch.getTraceCode() != null) {
                return "redirect:/trace/" + batch.getTraceCode() + "?complained=1&cid=" + complaint.getId();
            }
        }
        String keyword = complaint.getPhone() != null ? complaint.getPhone() : String.valueOf(complaint.getId());
        return "redirect:/complaint/query?keyword=" + keyword + "&submitted=1&cid=" + complaint.getId();
    }

    /**
     * 消费者"我的投诉"查询入口：跳转 Vue SPA 查询页（旧地址保持兼容）
     */
    @GetMapping("/complaint/query")
    public String complaintQueryPage(@RequestParam(required = false) String keyword) {
        return "redirect:/app/complaint/query";
    }

    @PostMapping("/complaint/query")
    public String complaintQuery(String keyword, Model model) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return "redirect:/app/complaint/query";
        }
        List<Complaint> list = complaintMapper.selectByPhoneOrCode(keyword.trim());
        model.addAttribute("complaints", list);
        model.addAttribute("keyword", keyword.trim());
        return "redirect:/app/complaint/query?keyword=" + keyword.trim();
    }

    /**
     * 消费者确认回复：仅"处理中"状态可确认，提交后直接关闭工单
     */
    @PostMapping("/complaint/{id}/confirm")
    public String confirmComplaint(@PathVariable Long id,
                                   @RequestParam String consumerReply,
                                   @RequestParam(required = false) String keyword) {
        Complaint c = complaintMapper.selectById(id);
        if (c != null && "PROCESSING".equals(c.getStatus())) {
            complaintMapper.consumerReply(id, consumerReply);
            log.info("消费者确认投诉[{}]，回复：{}，工单已关闭", id, consumerReply);
        }
        String kw = keyword != null && !keyword.isEmpty() ? keyword : (c != null ? c.getPhone() : "");
        return "redirect:/complaint/query?keyword=" + kw;
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
