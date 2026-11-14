package org.example.nongye.controller.api;

import cn.hutool.crypto.SecureUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.example.nongye.common.ApiException;
import org.example.nongye.common.ApiResult;
import org.example.nongye.common.AuthContext;
import org.example.nongye.common.PasswordHelper;
import org.example.nongye.entity.SysUser;
import org.example.nongye.mapper.SysUserMapper;
import org.example.nongye.service.AuthTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 认证与个人中心 REST 接口。
 * 身份优先取请求头 X-Auth-Token（支持同浏览器多标签页多角色），未携带时回退 HttpSession。
 */
@RestController
@RequestMapping("/api/auth")
public class ApiAuthController {

    private static final Logger log = LoggerFactory.getLogger(ApiAuthController.class);

    @Autowired
    private SysUserMapper userMapper;
    @Autowired
    private PasswordHelper passwordHelper;
    @Autowired
    private AuthTokenService tokenService;

    @Value("${upload.path}")
    private String uploadPath;

    private String tokenOf(HttpServletRequest request) {
        return request == null ? null : request.getHeader("X-Auth-Token");
    }

    /** 当前登录用户；未登录抛 401 */
    private SysUser current(HttpSession session) {
        SysUser user = AuthContext.user();
        if (user == null) {
            user = (SysUser) session.getAttribute("loginUser");
        }
        if (user == null) {
            throw ApiException.unauthorized("请先登录");
        }
        return user;
    }

    /** 脱敏后的用户信息（不含密码） */
    private Map<String, Object> view(SysUser u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", u.getId());
        m.put("username", u.getUsername());
        m.put("realName", u.getRealName());
        m.put("role", u.getRole());
        m.put("phone", u.getPhone());
        m.put("address", u.getAddress());
        m.put("avatar", u.getAvatar());
        return m;
    }

    @PostMapping("/login")
    public ApiResult<Map<String, Object>> login(@RequestBody Map<String, String> body, HttpSession session) {
        String username = body.getOrDefault("username", "").trim();
        String password = body.getOrDefault("password", "");
        String role = body.getOrDefault("role", "");
        if (username.isEmpty() || password.isEmpty() || role.isEmpty()) {
            throw ApiException.badRequest("请填写账号、密码并选择角色");
        }
        SysUser user = userMapper.selectByUsername(username);
        if (user == null || !passwordHelper.matches(password, user.getPassword())) {
            throw ApiException.badRequest("账号或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw ApiException.badRequest("账号已禁用");
        }
        if (!role.equals(user.getRole())) {
            throw ApiException.badRequest("该账号不属于当前登录通道，请选择正确的角色入口");
        }
        // 兼容历史裸 MD5：登录成功即迁移为 BCrypt
        if (!passwordHelper.isBcrypt(user.getPassword())) {
            userMapper.updatePassword(user.getId(), passwordHelper.encode(password));
        }
        session.setAttribute("loginUser", user);
        // 签发 token：前端按标签页保存，多角色可同时在线
        String token = tokenService.issue(user);
        Map<String, Object> data = view(user);
        data.put("token", token);
        log.info("用户登录成功：{}({})", username, user.getRole());
        return ApiResult.ok(data);
    }

    @PostMapping("/logout")
    public ApiResult<Void> logout(HttpServletRequest request, HttpSession session) {
        SysUser current = AuthContext.user();
        if (current != null) {
            log.info("用户登出：{}", current.getUsername());
        }
        tokenService.revoke(tokenOf(request));
        // 只清理后台登录身份，不销毁整个会话：同浏览器其他标签页的消费者身份不受影响
        session.removeAttribute("loginUser");
        return ApiResult.ok();
    }

    @GetMapping("/me")
    public ApiResult<Map<String, Object>> me(HttpSession session) {
        SysUser user = current(session);
        return ApiResult.ok(view(userMapper.selectById(user.getId())));
    }

    @PutMapping("/profile")
    public ApiResult<Map<String, Object>> updateProfile(@RequestBody Map<String, String> body, HttpServletRequest request, HttpSession session) {
        SysUser user = current(session);
        String realName = body.getOrDefault("realName", "").trim();
        if (realName.isEmpty()) {
            throw ApiException.badRequest("姓名不能为空");
        }
        user.setRealName(realName);
        user.setPhone(trimOrNull(body.get("phone")));
        user.setAddress(trimOrNull(body.get("address")));
        userMapper.updateProfile(user);
        SysUser fresh = userMapper.selectById(user.getId());
        session.setAttribute("loginUser", fresh);
        tokenService.refresh(tokenOf(request), fresh);
        log.info("用户[{}]更新个人信息", fresh.getUsername());
        return ApiResult.ok(view(fresh));
    }

    @PostMapping("/avatar")
    public ApiResult<Map<String, Object>> uploadAvatar(@RequestParam("file") MultipartFile file,
                                                       HttpServletRequest request, HttpSession session) throws Exception {
        SysUser user = current(session);
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("请选择头像图片");
        }
        String original = file.getOriginalFilename();
        String ext = original != null && original.contains(".")
                ? original.substring(original.lastIndexOf('.'))
                : "";
        if (!ext.toLowerCase().matches("\\.(png|jpg|jpeg|gif)")) {
            throw ApiException.badRequest("仅支持 PNG/JPG/GIF 图片");
        }
        File dir = new File(uploadPath, "avatar");
        dir.mkdirs();
        String fileName = UUID.randomUUID().toString().replace("-", "") + ext;
        // 必须用绝对路径：MultipartFile.transferTo 的相对路径会被解析到 Tomcat 临时目录
        file.transferTo(new File(dir, fileName).getAbsoluteFile());

        String url = "/uploads/avatar/" + fileName;
        userMapper.updateAvatar(user.getId(), url);
        SysUser fresh = userMapper.selectById(user.getId());
        session.setAttribute("loginUser", fresh);
        tokenService.refresh(tokenOf(request), fresh);
        log.info("用户[{}]更新头像", fresh.getUsername());
        return ApiResult.ok(Map.of("avatar", url));
    }

    @PutMapping("/password")
    public ApiResult<Void> updatePassword(@RequestBody Map<String, String> body, HttpSession session) {
        SysUser user = current(session);
        String oldPassword = body.getOrDefault("oldPassword", "");
        String newPassword = body.getOrDefault("newPassword", "");
        SysUser dbUser = userMapper.selectById(user.getId());
        if (!passwordHelper.matches(oldPassword, dbUser.getPassword())) {
            throw ApiException.badRequest("原密码不正确");
        }
        if (newPassword.length() < 6) {
            throw ApiException.badRequest("新密码长度至少 6 位");
        }
        userMapper.updatePassword(user.getId(), passwordHelper.encode(newPassword));
        log.info("用户[{}]修改登录密码", user.getUsername());
        return ApiResult.ok();
    }

    private String trimOrNull(String s) {
        return s == null || s.trim().isEmpty() ? null : s.trim();
    }
}
