package org.example.nongye.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 静态资源映射 + 登录拦截器注册
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${upload.path}")
    private String uploadPath;

    @Autowired
    private LoginInterceptor loginInterceptor;

    /**
     * 把上传文件目录映射成可访问 URL
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 统一解析为绝对路径，避免相对路径在不同启动方式下解析不一致
        java.io.File dir = new java.io.File(uploadPath).getAbsoluteFile();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + dir.getAbsolutePath() + java.io.File.separator);
        // CSS/JS 禁用浏览器缓存（max-age=0）：页面样式改动后普通刷新即可生效，避免缓存旧样式
        registry.addResourceHandler("/css/**")
                .addResourceLocations("classpath:/static/css/")
                .setCachePeriod(0);
        registry.addResourceHandler("/js/**")
                .addResourceLocations("classpath:/static/js/")
                .setCachePeriod(0);

        // Vue SPA（前端构建产物 static/app/）：真实文件直接返回，
        // 其余路径回退 index.html 交给 vue-router 处理（history 模式刷新不 404）
        registry.addResourceHandler("/app/**")
                .addResourceLocations("classpath:/static/app/")
                .setCachePeriod(0)
                .resourceChain(true)
                .addResolver(new org.springframework.web.servlet.resource.PathResourceResolver() {
                    @Override
                    protected org.springframework.core.io.Resource getResource(String resourcePath,
                            org.springframework.core.io.Resource location) throws java.io.IOException {
                        // /app/ 目录入口（resourcePath 为空）统一回退 index.html
                        String path = (resourcePath == null || resourcePath.isBlank() || resourcePath.endsWith("/"))
                                ? "index.html" : resourcePath;
                        org.springframework.core.io.Resource requested = location.createRelative(path);
                        if (requested.exists() && requested.isReadable()) {
                            return requested;
                        }
                        org.springframework.core.io.Resource index = location.createRelative("index.html");
                        return index.exists() && index.isReadable() ? index : null;
                    }
                });
    }

    /**
     * 注册登录拦截器，排除公开路径
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/", "/login", "/logout",
                        "/favicon.ico", "/qrcode",
                        "/trace/**", "/complaint/**",
                        "/api/ai/**",
                        "/error",
                        "/uploads/**", "/static/**", "/webjars/**", "/css/**", "/js/**", "/img/**"
                );
    }

    /**
     * 旧版 Thymeleaf 后台页面（/farmer、/dealer、/admin、/profile、/password 开头的 GET 请求）
     * 统一 302 重定向到新版 Vue SPA 对应页面，避免误入样式不再维护的旧页面。
     * 表单 POST 提交不受影响。
     */
    @org.springframework.context.annotation.Bean
    public org.springframework.boot.web.servlet.FilterRegistrationBean<org.springframework.web.filter.OncePerRequestFilter> legacyPageRedirectFilter() {
        org.springframework.web.filter.OncePerRequestFilter filter = new org.springframework.web.filter.OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(jakarta.servlet.http.HttpServletRequest request,
                                            jakarta.servlet.http.HttpServletResponse response,
                                            jakarta.servlet.FilterChain chain) throws jakarta.servlet.ServletException, java.io.IOException {
                String uri = request.getRequestURI();
                // 角色根路径带尾斜杠（/admin/、/farmer/、/dealer/）交给 LegacyRedirectController 处理，
                // 否则会被拼成 /app/admin/ 等 SPA 中不存在的路由
                boolean roleRootSlash = "/admin/".equals(uri) || "/farmer/".equals(uri) || "/dealer/".equals(uri);
                // 导出接口返回文件流，不能被当成旧页面重定向
                boolean legacyPage = "GET".equalsIgnoreCase(request.getMethod())
                        && !uri.endsWith("/export")
                        && !roleRootSlash
                        && (uri.startsWith("/farmer/") || uri.startsWith("/dealer/") || uri.startsWith("/admin/")
                            || uri.startsWith("/profile") || uri.startsWith("/password"));
                if (legacyPage) {
                    // 个人中心旧页在 SPA 中无对应路由，统一回登录页
                    if (uri.startsWith("/profile") || uri.startsWith("/password")) {
                        response.sendRedirect("/app/login");
                        return;
                    }
                    String query = request.getQueryString();
                    response.sendRedirect("/app" + uri + (query == null ? "" : "?" + query));
                    return;
                }
                chain.doFilter(request, response);
            }
        };
        org.springframework.boot.web.servlet.FilterRegistrationBean<org.springframework.web.filter.OncePerRequestFilter> bean =
                new org.springframework.boot.web.servlet.FilterRegistrationBean<>(filter);
        bean.addUrlPatterns("/farmer/*", "/farmer/*/*", "/dealer/*", "/dealer/*/*",
                "/admin/*", "/admin/*/*", "/profile", "/profile/*", "/password", "/password/*");
        bean.setOrder(1);
        return bean;
    }
}
