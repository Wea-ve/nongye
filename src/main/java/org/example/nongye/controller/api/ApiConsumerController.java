package org.example.nongye.controller.api;

import jakarta.servlet.http.HttpSession;
import org.example.nongye.common.ApiResult;
import org.example.nongye.entity.ConsumerAccount;
import org.example.nongye.entity.ConsumerFollow;
import org.example.nongye.entity.ConsumerSubscription;
import org.example.nongye.mapper.ComplaintMapper;
import org.example.nongye.mapper.ConsumerSubscriptionMapper;
import org.example.nongye.service.ConsumerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 信任者（消费者）端 REST 接口
 * 统一 session 键：consumerId / consumerPhone
 * 设计原则：公开页面调用时如果未注册，返回业务错误（ApiResult.error）而非 HTTP 401，
 * 避免前端 axios 拦截器强制 location.href = '/app/login'。
 */
@RestController
@RequestMapping("/api/consumer")
public class ApiConsumerController {

    private static final Logger log = LoggerFactory.getLogger(ApiConsumerController.class);

    @Autowired
    private ConsumerService consumerService;
    @Autowired
    private ConsumerSubscriptionMapper consumerSubscriptionMapper;
    @Autowired
    private ComplaintMapper complaintMapper;

    /** 从 session 取 consumerId；未注册返回 null */
    private Long findConsumerId(HttpSession session) {
        Object id = session.getAttribute("consumerId");
        if (id == null) return null;
        return ((Number) id).longValue();
    }

    // ==================== 账户 ====================

    @PostMapping("/register")
    public ApiResult<Map<String, Object>> register(@RequestBody Map<String, String> body, HttpSession session) {
        String phone = body.getOrDefault("phone", "").trim();
        String nickname = body.get("nickname");
        String inviteCode = body.get("inviteCode");
        ConsumerAccount account = consumerService.register(phone, nickname, inviteCode);
        session.setAttribute("consumerId", account.getId());
        session.setAttribute("consumerPhone", phone);
        log.info("信任者注册/登录：{}", phone);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("account", account);
        return ApiResult.ok(data);
    }

    @PostMapping("/login")
    public ApiResult<Map<String, Object>> login(@RequestBody Map<String, String> body, HttpSession session) {
        String phone = body.getOrDefault("phone", "").trim();
        ConsumerAccount account = consumerService.login(phone);
        if (account == null) {
            return ApiResult.error(400, "该手机号尚未注册，请先注册");
        }
        session.setAttribute("consumerId", account.getId());
        session.setAttribute("consumerPhone", phone);
        log.info("信任者登录：{}", phone);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("account", account);
        return ApiResult.ok(data);
    }

    @PostMapping("/logout")
    public ApiResult<Void> logout(HttpSession session) {
        session.removeAttribute("consumerId");
        session.removeAttribute("consumerPhone");
        return ApiResult.ok();
    }

    /** 公开：未注册返回 data=null，前端据此显示注册引导 */
    @GetMapping("/me")
    public ApiResult<ConsumerAccount> me(HttpSession session) {
        Long id = findConsumerId(session);
        if (id == null) return ApiResult.ok(null);
        return ApiResult.ok(consumerService.me(id));
    }

    // ==================== 首页 ====================

    /** 公开：未注册返回空壳结构，前端显示"去注册"引导 */
    @GetMapping("/home")
    public ApiResult<Map<String, Object>> home(HttpSession session) {
        Long id = findConsumerId(session);
        if (id == null) {
            Map<String, Object> empty = new LinkedHashMap<>();
            empty.put("follows", List.of());
            empty.put("growing", List.of());
            empty.put("todos", List.of());
            empty.put("tip", "注册成为信任者，解锁专属消费助手 👇");
            empty.put("registered", false);
            return ApiResult.ok(empty);
        }
        Map<String, Object> home = consumerService.home(id);
        home.put("registered", true);
        return ApiResult.ok(home);
    }

    // ==================== 关注 ====================

    @PostMapping("/follow")
    public ApiResult<?> follow(@RequestBody Map<String, Object> body, HttpSession session) {
        Long consumerId = findConsumerId(session);
        if (consumerId == null) return ApiResult.error(400, "请先注册成为信任者");
        String targetType = (String) body.get("targetType");
        Object tid = body.get("targetId");
        Long targetId = tid instanceof Number ? ((Number) tid).longValue() : Long.valueOf(tid.toString());
        ConsumerFollow follow = consumerService.follow(consumerId, targetType, targetId);
        if (follow == null) return ApiResult.ok(null);
        return ApiResult.ok(follow);
    }

    @DeleteMapping("/follow/{id}")
    public ApiResult<?> unfollow(@PathVariable Long id, HttpSession session) {
        Long consumerId = findConsumerId(session);
        if (consumerId == null) return ApiResult.error(400, "请先注册成为信任者");
        return ApiResult.ok(consumerService.unfollow(consumerId, id));
    }

    // ==================== 订阅 ====================

    @PostMapping("/subscription")
    public ApiResult<?> createSubscription(@RequestBody Map<String, Object> body, HttpSession session) {
        Long consumerId = findConsumerId(session);
        if (consumerId == null) return ApiResult.error(400, "请先注册成为信任者");
        String cropName = (String) body.get("cropName");
        Object bid = body.get("batchId");
        Long batchId = bid == null ? null : (bid instanceof Number ? ((Number) bid).longValue() : Long.valueOf(bid.toString()));
        Object qtyObj = body.get("qty");
        BigDecimal qty = qtyObj == null ? null : new BigDecimal(qtyObj.toString());
        Object cycleObj = body.get("cycleDays");
        Integer cycleDays = cycleObj == null ? null : Integer.valueOf(cycleObj.toString());
        return ApiResult.ok(consumerService.createSubscription(consumerId, cropName, batchId, qty, cycleDays));
    }

    @GetMapping("/subscriptions")
    public ApiResult<?> subscriptions(HttpSession session) {
        Long consumerId = findConsumerId(session);
        if (consumerId == null) return ApiResult.ok(List.of());
        return ApiResult.ok(consumerSubscriptionMapper.selectByConsumerAndStatus(consumerId, "ACTIVE"));
    }

    // ==================== 事件 ====================

    @PostMapping("/event")
    public ApiResult<Void> logEvent(@RequestBody Map<String, Object> body, HttpSession session) {
        Long consumerId = findConsumerId(session);
        if (consumerId == null) return ApiResult.ok();
        String eventType = (String) body.get("eventType");
        Object bid = body.get("batchId");
        Long batchId = bid == null ? null : (bid instanceof Number ? ((Number) bid).longValue() : Long.valueOf(bid.toString()));
        String payload = body.get("payload") == null ? null : body.get("payload").toString();
        consumerService.logEvent(consumerId, eventType, batchId, payload);
        return ApiResult.ok();
    }

    // ==================== 售后 ====================

    @GetMapping("/aftersales")
    public ApiResult<?> aftersales(HttpSession session) {
        Long consumerId = findConsumerId(session);
        if (consumerId == null) return ApiResult.ok(List.of());
        return ApiResult.ok(complaintMapper.selectByConsumer(consumerId));
    }
}
