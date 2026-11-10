package org.example.nongye.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常处理：
 * - 登录/权限异常 → 跳登录页
 * - 业务异常 → 友好错误页（带返回按钮）
 * - 系统异常 → 友好错误页（通用提示）
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private boolean isAuthError(String msg) {
        return msg != null && (msg.contains("未登录") || msg.contains("无权限") || msg.contains("权限"));
    }

    @ExceptionHandler(RuntimeException.class)
    public ModelAndView handleRuntime(RuntimeException e) {
        String msg = e.getMessage();
        if (isAuthError(msg)) {
            // 权限/登录异常：跳 SPA 登录页
            log.warn("鉴权异常：{}", msg);
            return new ModelAndView("redirect:/app/login");
        }
        // 业务异常：友好错误页
        log.warn("业务异常：{}", msg);
        ModelAndView mv = new ModelAndView("error_page");
        mv.addObject("title", "操作失败");
        mv.addObject("msg", msg != null ? msg : "操作出现异常");
        mv.addObject("showLogin", false);
        return mv;
    }

    /**
     * 静态资源不存在（如 favicon.ico）：静默返回 404，不刷 ERROR 日志
     */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ModelAndView handleNotFound(NoResourceFoundException e) {
        log.info("静态资源不存在：{}", e.getResourcePath());
        ModelAndView mv = new ModelAndView("error_page");
        mv.addObject("title", "页面不存在");
        mv.addObject("msg", "您访问的页面不存在或已被移除");
        mv.addObject("showLogin", false);
        return mv;
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView handleAll(Exception e) {
        log.error("系统异常", e);
        ModelAndView mv = new ModelAndView("error_page");
        mv.addObject("title", "系统异常");
        mv.addObject("msg", "系统开小差了，请稍后重试");
        mv.addObject("showLogin", false);
        return mv;
    }
}
