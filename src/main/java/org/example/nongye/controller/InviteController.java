package org.example.nongye.controller;

import org.example.nongye.entity.CropBatch;
import org.example.nongye.entity.SysUser;
import org.example.nongye.mapper.CropBatchMapper;
import org.example.nongye.mapper.SysUserMapper;
import org.example.nongye.service.TrustScoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 邀请链接入口：旧版 Thymeleaf 重定向 + 新版 SPA 专用的公开 REST。
 * 邀请码匹配优先级：SysUser.username → CropBatch.traceCode → CropBatch.batchNo。
 */
@Controller
@RequestMapping("/invite")
public class InviteController {

    @Autowired
    private SysUserMapper sysUserMapper;
    @Autowired
    private CropBatchMapper batchMapper;
    @Autowired
    private TrustScoreService trustScoreService;

    /** 旧版 Thymeleaf 入口：找到 inviter 后重定向到 SPA 路由 */
    @GetMapping("/{inviteCode}")
    public String invitePage(@PathVariable String inviteCode, Model model) {
        Object inviter = resolveInviter(inviteCode);
        model.addAttribute("inviter", inviter);
        return "redirect:/app/invite/" + inviteCode;
    }

    /** SPA 专用公开 REST：返回邀请人基本信息 + 可信统计 */
    @RestController
    @RequestMapping("/api/invite")
    public static class InviteApi {

        @Autowired
        private SysUserMapper sysUserMapper;
        @Autowired
        private CropBatchMapper batchMapper;
        @Autowired
        private TrustScoreService trustScoreService;

        @GetMapping("/{inviteCode}")
        public Map<String, Object> inviteInfo(@PathVariable String inviteCode) {
            Map<String, Object> out = new LinkedHashMap<>();
            Map<String, Object> inviterMap = new LinkedHashMap<>();
            Map<String, Object> trustStats = new LinkedHashMap<>();

            SysUser user = sysUserMapper.selectByUsername(inviteCode);
            if (user != null) {
                inviterMap.put("name", user.getRealName() != null ? user.getRealName() : user.getUsername());
                inviterMap.put("role", user.getRole() != null ? user.getRole() : "farmer");
                inviterMap.put("inviterId", user.getId());
                // 该邀请人（农户）的所有批次的可信统计
                List<CropBatch> batches = batchMapper.selectByFarmerId(user.getId());
                trustStats = computeTrustStats(batches);
            } else {
                CropBatch batch = batchMapper.selectByTraceCode(inviteCode);
                if (batch == null) {
                    for (CropBatch b : batchMapper.selectAll()) {
                        if (inviteCode.equals(b.getBatchNo())) {
                            batch = b;
                            break;
                        }
                    }
                }
                if (batch != null) {
                    inviterMap.put("name", batch.getFarmerName() != null ? batch.getFarmerName() : "未知农户");
                    inviterMap.put("role", "farmer");
                    inviterMap.put("inviterId", batch.getFarmerId());
                    trustStats = computeTrustStats(List.of(batch));
                } else {
                    inviterMap.put("name", null);
                    inviterMap.put("role", null);
                    inviterMap.put("inviterId", null);
                }
            }

            out.put("inviter", inviterMap);
            out.put("trustStats", trustStats);
            return out;
        }

        private Map<String, Object> computeTrustStats(List<CropBatch> batches) {
            Map<String, Object> stats = new LinkedHashMap<>();
            Map<String, Integer> trustLevelCounts = new LinkedHashMap<>();
            double sum = 0.0;
            int count = 0;
            for (CropBatch b : batches) {
                Map<String, Object> t = trustScoreService.evaluate(b.getId());
                if (t == null) continue;
                Object score = t.get("score");
                Object level = t.get("level");
                if (score instanceof Number) {
                    sum += ((Number) score).doubleValue();
                    count++;
                }
                String lv = level != null ? level.toString() : "UNKNOWN";
                trustLevelCounts.merge(lv, 1, Integer::sum);
            }
            stats.put("trustLevelCounts", trustLevelCounts);
            stats.put("avgTrustScore", count > 0 ? Math.round((sum / count) * 10.0) / 10.0 : 0.0);
            return stats;
        }
    }

    /** 本地 Thymeleaf 入口复用：寻找邀请者对象 */
    private Object resolveInviter(String inviteCode) {
        SysUser user = sysUserMapper.selectByUsername(inviteCode);
        if (user != null) return user;
        CropBatch batch = batchMapper.selectByTraceCode(inviteCode);
        if (batch != null) return batch;
        for (CropBatch b : batchMapper.selectAll()) {
            if (inviteCode.equals(b.getBatchNo())) return b;
        }
        return null;
    }
}
