package org.example.nongye.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.nongye.entity.SysUser;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.HashMap;
import java.util.Map;

/**
 * 登录与角色拦截器：未登录跳转登录页，角色不符跳转其首页
 */
@Component
public class LoginInterceptor implements HandlerInterceptor {

    /** 路径前缀 -> 必需角色 */
    private static final Map<String, String> ROLE_PATH = new HashMap<>();

    static {
        ROLE_PATH.put("/farmer", "farmer");
        ROLE_PATH.put("/dealer", "dealer");
        ROLE_PATH.put("/admin", "admin");
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();

        // 找到匹配的角色前缀
        String requiredRole = null;
        for (Map.Entry<String, String> e : ROLE_PATH.entrySet()) {
            if (uri.startsWith(e.getKey())) {
                requiredRole = e.getValue();
                break;
            }
        }
        // 不受角色保护的路径直接放行
        if (requiredRole == null) {
            return true;
        }

        SysUser user = org.example.nongye.common.AuthContext.user();
        if (user == null) {
            user = (SysUser) request.getSession().getAttribute("loginUser");
        }
        if (user == null) {
            response.sendRedirect("/login");
            return false;
        }
        if (!requiredRole.equals(user.getRole())) {
            // 角色不符，跳转到其自身首页，避免越权
            response.sendRedirect("/" + user.getRole() + "/dashboard");
            return false;
        }
        return true;
    }
}
