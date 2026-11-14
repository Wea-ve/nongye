package org.example.nongye.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.nongye.common.AuthContext;
import org.example.nongye.entity.SysUser;
import org.example.nongye.service.AuthTokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 把请求头 X-Auth-Token 解析为当前用户写入 AuthContext。
 * 未携带 token 时保持原 session 逻辑（兼容旧页面/导出等场景）。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TokenAuthFilter extends OncePerRequestFilter {

    @Autowired
    private AuthTokenService tokenService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String token = request.getHeader("X-Auth-Token");
        try {
            SysUser user = tokenService.user(token);
            if (user != null) {
                AuthContext.set(user);
            }
            chain.doFilter(request, response);
        } finally {
            AuthContext.clear();
        }
    }
}
