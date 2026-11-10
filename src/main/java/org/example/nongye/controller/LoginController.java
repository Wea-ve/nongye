package org.example.nongye.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * 旧登录入口：统一跳转到 Vue SPA 登录页
 * 实际登录由 /api/auth/login 处理（带 role 参数校验角色通道）。
 */
@Controller
public class LoginController {

    @GetMapping({"/", "/login"})
    public String loginPage() {
        return "redirect:/app/login";
    }

    /** 旧表单提交入口已废弃，统一回 SPA */
    @PostMapping("/login")
    public String loginSubmit() {
        return "redirect:/app/login";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/app/login";
    }
}
