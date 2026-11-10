package org.example.nongye.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 旧版根路径兜底（/admin、/farmer、/dealer）
 * 子路径由 WebConfig 的 legacyPageRedirectFilter 统一 302 到 /app 对应页面；
 * 这里只处理过滤器 urlPatterns 覆盖不到的根路径（"/admin/*" 不匹配 "/admin"）。
 */
@Controller
public class LegacyRedirectController {

    @GetMapping({"/admin", "/admin/"})
    public String adminRoot() {
        return "redirect:/app/admin/dashboard";
    }

    @GetMapping({"/farmer", "/farmer/"})
    public String farmerRoot() {
        return "redirect:/app/farmer/dashboard";
    }

    @GetMapping({"/dealer", "/dealer/"})
    public String dealerRoot() {
        return "redirect:/app/dealer/dashboard";
    }
}
