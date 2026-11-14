package org.example.nongye.common;

import org.example.nongye.entity.SysUser;

/**
 * 请求级登录上下文：由 TokenAuthFilter 按 X-Auth-Token 解析后写入，请求结束清理。
 * 作用：同一浏览器多标签页可各自携带不同角色 token，互不覆盖。
 */
public final class AuthContext {

    private static final ThreadLocal<SysUser> USER = new ThreadLocal<>();

    private AuthContext() {
    }

    public static void set(SysUser user) {
        USER.set(user);
    }

    public static SysUser user() {
        return USER.get();
    }

    public static void clear() {
        USER.remove();
    }
}
