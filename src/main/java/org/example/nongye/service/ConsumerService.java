package org.example.nongye.service;

import org.example.nongye.common.ApiException;
import org.example.nongye.entity.*;
import org.example.nongye.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

        List<Map<String, Object>> followList = getConsumerFollows(consumerId);

        // growing：关注且状态 PLANTING/HARVESTED 的批次
        List<Map<String, Object>> growing = new ArrayList<>();
        for (ConsumerFollow f : follows) {
            if ("BATCH".equals(f.getTargetType())) {
                CropBatch b = cropBatchMapper.selectById(f.getTargetId());
                if (b != null && ("PLANTING".equals(b.getStatus()) || "HARVESTED".equals(b.getStatus()))) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("batchId", b.getId());
                    m.put("batchNo", b.getBatchNo());
                    m.put("cropName", b.getCropName());
                    m.put("farmName", b.getFarmerName());
                    m.put("status", b.getStatus());
                    m.put("expectHarvestDate", b.getExpectHarvestDate());
                    growing.add(m);
                }
            }
        }

        // todos：今日到期的订阅 + 未关闭的投诉（用 LinkedHashMap 装两条）
        List<Map<String, Object>> todos = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (ConsumerSubscription s : subs) {
            if (s.getNextDate() != null && !s.getNextDate().isAfter(today)) {
                Map<String, Object> t = new LinkedHashMap<>();
                t.put("type", "SUBSCRIPTION_DUE");
                t.put("subscriptionId", s.getId());
                t.put("cropName", s.getCropName());
                t.put("nextDate", s.getNextDate());
                todos.add(t);
            }
        }
        List<Complaint> complaints = complaintMapper.selectByConsumer(consumerId);
        for (Complaint c : complaints) {
            if (c.getStatus() != null && !"CLOSED".equalsIgnoreCase(c.getStatus())) {
                Map<String, Object> t = new LinkedHashMap<>();
                t.put("type", "COMPLAINT_OPEN");
                t.put("complaintId", c.getId());
                t.put("status", c.getStatus());
                t.put("content", c.getContent());
                todos.add(t);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("follows", followList);
        result.put("growing", growing);
        result.put("todos", todos);
        int month = LocalDate.now().getMonthValue();
        String tip = String.format("当前是 %d 月，多关注应季果蔬，吃得新鲜又健康！", month);
        result.put("tip", tip);
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
}
