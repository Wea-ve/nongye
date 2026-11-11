package org.example.nongye.service;

import org.example.nongye.common.ApiException;
import org.example.nongye.entity.*;
import org.example.nongye.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 信任者（消费者）账户核心服务
 */
@Service
public class ConsumerService {

    @Autowired
    private ConsumerAccountMapper consumerAccountMapper;
    @Autowired
    private ConsumerFollowMapper consumerFollowMapper;
    @Autowired
    private ConsumerSubscriptionMapper consumerSubscriptionMapper;
    @Autowired
    private ConsumerEventMapper consumerEventMapper;
    @Autowired
    private CropBatchMapper cropBatchMapper;
    @Autowired
    private SysUserMapper sysUserMapper;
    @Autowired
    private ComplaintMapper complaintMapper;
    @Autowired
    private TrustPromiseMapper trustPromiseMapper;
    @Autowired
    private TrustPromiseService trustPromiseService;

    // ==================== 账户 ====================

    /**
     * 注册/登录合一：按手机号查，存在则更新 lastActiveTime 直接返回；
     * 不存在则插入新账户（trust_level=FOLLOWER），inviteCode 存在时查 SysUser.username 作为 inviter。
     */
    public ConsumerAccount register(String phone, String nickname, String inviteCode) {
        if (phone == null || phone.trim().isEmpty()) {
            throw ApiException.badRequest("手机号不能为空");
        }
        phone = phone.trim();

        ConsumerAccount account = consumerAccountMapper.selectByPhone(phone);
        if (account != null) {
            consumerAccountMapper.updateLastActive(account.getId());
            logEvent(account.getId(), "REGISTER", null, "registered-existing");
            return account;
        }

        account = new ConsumerAccount();
        account.setPhone(phone);
        account.setNickname(nickname != null && !nickname.trim().isEmpty()
                ? nickname.trim() : "信任者" + phone.substring(phone.length() - 4));
        account.setTrustLevel("FOLLOWER");
        account.setStatus(1);
        account.setLastActiveTime(LocalDateTime.now());
        account.setCreateTime(LocalDateTime.now());

        if (inviteCode != null && !inviteCode.trim().isEmpty()) {
            String code = inviteCode.trim();
            // 先查 SysUser.username 匹配（演示：把管理员账号 username 当邀请码）
            SysUser inviterUser = sysUserMapper.selectByUsername(code);
            if (inviterUser != null) {
                account.setInviteCode(code);
                account.setInviterType(inviterUser.getRole() != null ? inviterUser.getRole() : "admin");
                account.setInviterId(inviterUser.getId());
            } else {
                // 兜底 inviterType
                account.setInviteCode(code);
                account.setInviterType("admin");
            }
        }

        consumerAccountMapper.insert(account);
        logEvent(account.getId(), "REGISTER", null, "registered-new");
        return account;
    }

    /**
     * 登录：按手机号查 + 更新 lastActiveTime，不存在返回 null
     */
    public ConsumerAccount login(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return null;
        }
        ConsumerAccount account = consumerAccountMapper.selectByPhone(phone.trim());
        if (account == null) {
            return null;
        }
        consumerAccountMapper.updateLastActive(account.getId());
        logEvent(account.getId(), "LOGIN", null, null);
        return account;
    }

    /** 按 id 查账户 */
    public ConsumerAccount me(Long consumerId) {
        return consumerAccountMapper.selectById(consumerId);
    }

    // ==================== 关注 ====================

    /**
     * 关注：已存在则返回 null；不存在则 insert + 写 FOLLOW 事件
     */
    public ConsumerFollow follow(Long consumerId, String targetType, Long targetId) {
        if (targetType == null || targetId == null) {
            throw ApiException.badRequest("targetType / targetId 不能为空");
        }
        ConsumerFollow exist = consumerFollowMapper.selectByConsumerAndTarget(consumerId, targetType, targetId);
        if (exist != null) {
            return null;
        }
        ConsumerFollow follow = new ConsumerFollow();
        follow.setConsumerId(consumerId);
        follow.setTargetType(targetType);
        follow.setTargetId(targetId);
        follow.setNotifyEnabled(1);
        follow.setCreateTime(LocalDateTime.now());
        consumerFollowMapper.insert(follow);
        logEvent(consumerId, "FOLLOW", targetId, "targetType=" + targetType);
        return follow;
    }

    /** 取消关注：先校验 follow 属于该 consumer 再 delete；返回是否删除成功 */
    public boolean unfollow(Long consumerId, Long followId) {
        List<ConsumerFollow> all = consumerFollowMapper.selectByConsumer(consumerId);
        ConsumerFollow target = all.stream()
                .filter(x -> Objects.equals(x.getId(), followId))
                .findFirst()
                .orElse(null);
        if (target == null) {
            return false;
        }
        consumerFollowMapper.deleteById(followId);
        logEvent(consumerId, "UNFOLLOW", target.getTargetId(), "targetType=" + target.getTargetType());
        return true;
    }

    // ==================== 首页聚合 ====================

    /**
     * 首页聚合：follows / growing(关注且 PLANTING/HARVESTED 批次) /
     * todos(今日到期 subscription + ACTIVE 未关闭 complaint) / tip。
     */
    public Map<String, Object> home(Long consumerId) {
        List<ConsumerFollow> follows = consumerFollowMapper.selectByConsumer(consumerId);
        List<ConsumerSubscription> subs = consumerSubscriptionMapper.selectByConsumer(consumerId);

        // 关注清单：targetName / days / buyCount / notifyEnabled（与前端 HomeView 字段对齐）
        List<Map<String, Object>> followList = new ArrayList<>();
        for (ConsumerFollow f : follows) {
            Map<String, Object> m = new LinkedHashMap<>();
            String targetName = null;
            String traceCode = null;
            if ("BATCH".equals(f.getTargetType())) {
                CropBatch b = cropBatchMapper.selectById(f.getTargetId());
                if (b != null) {
                    targetName = (b.getFarmerName() == null ? "" : b.getFarmerName() + " 的") + b.getCropName();
                    traceCode = b.getTraceCode();
                }
            } else if ("FARMER".equals(f.getTargetType())) {
                SysUser u = sysUserMapper.selectById(f.getTargetId());
                if (u != null) targetName = u.getRealName() != null ? u.getRealName() : u.getUsername();
            }
            long days = f.getCreateTime() == null ? 0
                    : Math.max(0, ChronoUnit.DAYS.between(f.getCreateTime().toLocalDate(), LocalDate.now()));
            long buyCount = subs.stream().filter(s -> Objects.equals(f.getTargetId(), s.getBatchId())).count();
            m.put("targetType", f.getTargetType());
            m.put("targetId", f.getTargetId());
            m.put("targetName", targetName != null ? targetName : "（已下架）");
            m.put("traceCode", traceCode);
            m.put("days", days);
            m.put("buyCount", buyCount);
            m.put("notifyEnabled", f.getNotifyEnabled() != null && f.getNotifyEnabled() == 1);
            followList.add(m);
        }

        // growing：关注且处于种植/生长/已采收的批次
        List<Map<String, Object>> growing = new ArrayList<>();
        for (ConsumerFollow f : follows) {
            if (!"BATCH".equals(f.getTargetType())) continue;
            CropBatch b = cropBatchMapper.selectById(f.getTargetId());
            if (b == null || b.getStatus() == null) continue;
            if (!("PLANTING".equals(b.getStatus()) || "GROWING".equals(b.getStatus()) || "HARVESTED".equals(b.getStatus()))) {
                continue;
            }
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", b.getId());
            m.put("batchId", b.getId());
            m.put("traceCode", b.getTraceCode());
            m.put("cropName", b.getCropName());
            m.put("farmerName", b.getFarmerName());
            m.put("status", b.getStatus());
            m.put("expectDate", b.getExpectHarvestDate() == null ? null : b.getExpectHarvestDate().toString());
            m.put("notifyEnabled", f.getNotifyEnabled() != null && f.getNotifyEnabled() == 1);
            m.put("promises", promiseJudgments(b.getId()));
            growing.add(m);
        }

        // todos：今日到期的订阅 + 未关闭的投诉（type=subscribe/complaint）
        List<Map<String, Object>> todos = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (ConsumerSubscription s : subs) {
            if (s.getNextDate() != null && !s.getNextDate().isAfter(today)) {
                Map<String, Object> t = new LinkedHashMap<>();
                t.put("type", "subscribe");
                t.put("id", s.getId());
                t.put("subscriptionId", s.getId());
                t.put("text", "该买「" + s.getCropName() + "」了");
                t.put("nextDate", s.getNextDate().toString());
                todos.add(t);
            }
        }
        for (Complaint c : complaintMapper.selectByConsumer(consumerId)) {
            if (c.getStatus() != null && !"CLOSED".equalsIgnoreCase(c.getStatus())) {
                Map<String, Object> t = new LinkedHashMap<>();
                t.put("type", "complaint");
                t.put("complaintId", c.getId());
                String content = c.getContent() == null ? "售后处理中" : c.getContent();
                t.put("text", content.length() > 20 ? content.substring(0, 20) + "…" : content);
                t.put("slaHours", c.getSlaHours() == null ? 24 : c.getSlaHours());
                t.put("slaPassedHours", c.getCreateTime() == null ? 0
                        : Math.max(0, ChronoUnit.HOURS.between(c.getCreateTime(), LocalDateTime.now())));
                todos.add(t);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("follows", followList);
        result.put("growing", growing);
        result.put("todos", todos);
        int month = LocalDate.now().getMonthValue();
        result.put("tip", String.format("当前是 %d 月，多关注应季果蔬，吃得新鲜又健康！", month));
        return result;
    }

    /**
     * 消费者关注列表：联表 SysUser/CropBatch 补 farmName/batchNo/cropName
     */
    public List<Map<String, Object>> getConsumerFollows(Long consumerId) {
        List<ConsumerFollow> follows = consumerFollowMapper.selectByConsumer(consumerId);
        return follows.stream().map(f -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", f.getId());
            m.put("targetType", f.getTargetType());
            m.put("targetId", f.getTargetId());
            if ("BATCH".equals(f.getTargetType())) {
                CropBatch b = cropBatchMapper.selectById(f.getTargetId());
                if (b != null) {
                    m.put("farmName", b.getFarmerName());
                    m.put("batchNo", b.getBatchNo());
                    m.put("cropName", b.getCropName());
                    m.put("status", b.getStatus());
                }
            } else if ("FARMER".equals(f.getTargetType())) {
                SysUser u = sysUserMapper.selectById(f.getTargetId());
                if (u != null) {
                    m.put("farmName", u.getRealName());
                }
            }
            return m;
        }).collect(Collectors.toList());
    }

    // ==================== 订阅 ====================

    /** 创建订阅 */
    public ConsumerSubscription createSubscription(Long consumerId, String cropName,
                                                  Long batchId, BigDecimal qty, Integer cycleDays) {
        if (cropName == null || cropName.trim().isEmpty()) {
            throw ApiException.badRequest("作物名不能为空");
        }
        ConsumerSubscription s = new ConsumerSubscription();
        s.setConsumerId(consumerId);
        s.setCropName(cropName.trim());
        s.setBatchId(batchId);
        s.setQty(qty != null ? qty : BigDecimal.ONE);
        s.setUnit("斤");
        s.setCycleDays(cycleDays != null ? cycleDays : 7);
        s.setNextDate(LocalDate.now().plusDays(s.getCycleDays()));
        s.setStatus("ACTIVE");
        s.setCreateTime(LocalDateTime.now());
        consumerSubscriptionMapper.insert(s);
        logEvent(consumerId, "SUBSCRIBE", batchId, "crop=" + cropName);
        return s;
    }

    // ==================== 事件 ====================

    /** 写入消费者事件 */
    public void logEvent(Long consumerId, String eventType, Long batchId, String payload) {
        ConsumerEvent e = new ConsumerEvent();
        e.setConsumerId(consumerId);
        e.setEventType(eventType);
        e.setBatchId(batchId);
        e.setPayload(payload);
        e.setCreateTime(LocalDateTime.now());
        consumerEventMapper.insert(e);
    }

    // ==================== 承诺兑现 ====================

    /** 批次承诺的兑现判定（供信任者首页/侧栏展示） */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> promiseJudgments(Long batchId) {
        List<Map<String, Object>> out = new ArrayList<>();
        Map<String, Object> fulfillment = trustPromiseService.fulfillment(batchId);
        Object raw = fulfillment.get("judgments");
        if (!(raw instanceof List)) return out;
        for (Object o : (List<Object>) raw) {
            Map<String, Object> j = (Map<String, Object>) o;
            TrustPromise p = (TrustPromise) j.get("promise");
            if (p == null) continue;
            String judgment = String.valueOf(j.get("judgment"));
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", p.getId());
            item.put("type", p.getPromiseType());
            item.put("typeText", promiseTypeText(p.getPromiseType()));
            item.put("value", p.getPromiseValue());
            item.put("status", judgment);
            item.put("statusText", switch (judgment) {
                case "FULFILLED" -> "已兑现";
                case "FAILED" -> "未达成";
                default -> "待验证";
            });
            item.put("remark", j.get("remark"));
            out.add(item);
        }
        return out;
    }

    private String promiseTypeText(String type) {
        if (type == null) return "承诺";
        return switch (type.toUpperCase()) {
            case "FRESH" -> "新鲜承诺";
            case "HARVEST_DATE" -> "采收日期承诺";
            case "QUALITY" -> "品质承诺";
            default -> "承诺";
        };
    }
}
