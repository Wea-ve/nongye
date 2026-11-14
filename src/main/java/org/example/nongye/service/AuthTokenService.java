package org.example.nongye.service;

import org.example.nongye.entity.SysUser;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录令牌服务：登录签发随机 token，接口通过 X-Auth-Token 携带。
 * 相比 HttpSession，token 由前端按标签页保存，同一浏览器可同时登录多个角色互不干扰。
 */
@Service
public class AuthTokenService {

    /** token 有效期：12 小时 */
    private static final long TTL_MILLIS = 12L * 60 * 60 * 1000;

    private final Map<String, TokenEntry> tokens = new ConcurrentHashMap<>();

    public String issue(SysUser user) {
        pruneExpired();
        String token = UUID.randomUUID().toString().replace("-", "");
        tokens.put(token, new TokenEntry(user, System.currentTimeMillis() + TTL_MILLIS));
        return token;
    }

    public SysUser user(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        TokenEntry entry = tokens.get(token);
        if (entry == null) {
            return null;
        }
        if (entry.expireAt < System.currentTimeMillis()) {
            tokens.remove(token);
            return null;
        }
        return entry.user;
    }

    public void revoke(String token) {
        if (token != null && !token.isBlank()) {
            tokens.remove(token);
        }
    }

    /** 用户资料变更后刷新 token 中缓存的用户信息 */
    public void refresh(String token, SysUser user) {
        if (token == null || token.isBlank()) {
            return;
        }
        TokenEntry entry = tokens.get(token);
        if (entry != null) {
            tokens.put(token, new TokenEntry(user, entry.expireAt));
        }
    }

    private void pruneExpired() {
        long now = System.currentTimeMillis();
        tokens.entrySet().removeIf(e -> e.getValue().expireAt < now);
    }

    private record TokenEntry(SysUser user, long expireAt) {
    }
}
