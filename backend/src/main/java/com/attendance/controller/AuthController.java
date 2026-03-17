package com.attendance.controller;

import com.attendance.common.Result;
import com.attendance.entity.User;
import com.attendance.security.JwtTokenUtil;
import com.attendance.service.UserService;
import com.attendance.util.PasswordUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Value("${wechat.appid}")
    private String wxAppId;

    @Value("${wechat.secret}")
    private String wxSecret;

    @PostMapping("/register")
    public Result<?> register(@RequestBody User user) {
        if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            return Result.error("账号不能为空");
        }
        if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
            return Result.error("密码不能为空");
        }
        if (userService.findByUsername(user.getUsername()) != null) {
            return Result.error("账号已存在");
        }

        String salt = PasswordUtil.generateSalt();
        String encoded = PasswordUtil.encode(user.getPassword(), salt);
        user.setSalt(salt);
        user.setPassword(encoded);
        user.setRoleType(0);
        user.setStatus(1);
        user.setRegisterStatus(0);
        user.setLoginFailCount(0);
        user.setLockUntil(null);

        userService.save(user);
        return Result.success("注册申请已提交，请等待管理员审核");
    }

    @PostMapping("/login")
    public Result<?> login(@RequestBody Map<String, String> loginRequest) {
        String username = loginRequest.get("username");
        String password = loginRequest.get("password");

        if (username == null || username.trim().isEmpty()) {
            return Result.error("账号不能为空");
        }
        if (password == null || password.trim().isEmpty()) {
            return Result.error("密码不能为空");
        }

        User user = userService.findByUsername(username);
        if (user == null) {
            return Result.error("用户不存在");
        }

        if (user.getStatus() != null && user.getStatus() == 0) {
            return Result.error("账号已被禁用");
        }
        if (user.getRegisterStatus() != null && user.getRegisterStatus() != 1) {
            return Result.error("账号未通过审核");
        }
        if (user.getLockUntil() != null && user.getLockUntil().isAfter(LocalDateTime.now())) {
            return Result.error("账号已被锁定，请稍后再试");
        }

        boolean ok = PasswordUtil.matches(password, user.getSalt(), user.getPassword());
        if (!ok) {
            int failCount = user.getLoginFailCount() == null ? 0 : user.getLoginFailCount();
            failCount++;
            user.setLoginFailCount(failCount);
            if (failCount >= 5) {
                user.setLockUntil(LocalDateTime.now().plusMinutes(15));
                user.setLoginFailCount(0);
            }
            userService.updateById(user);
            return Result.error("密码错误");
        }

        user.setLoginFailCount(0);
        user.setLockUntil(null);
        userService.updateById(user);

        String token = jwtTokenUtil.generateToken(user.getId(), user.getUsername());

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("employeeNo", user.getEmployeeNo());
        userInfo.put("name", user.getName());
        userInfo.put("departmentId", user.getDepartmentId());
        userInfo.put("departmentName", user.getDepartmentName());
        userInfo.put("position", user.getPosition());
        userInfo.put("phone", user.getPhone());
        userInfo.put("roleType", user.getRoleType());

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userInfo", userInfo);
        return Result.success(result);
    }

    /**
     * 微信一键登录：小程序调用wx.login()获取code，传给后端换取openid
     * 如果openid已绑定用户则直接登录，否则返回openid让前端引导绑定
     */
    @PostMapping("/wx-login")
    public Result<?> wxLogin(@RequestBody Map<String, String> req) {
        String code = req.get("code");
        if (code == null || code.trim().isEmpty()) {
            return Result.error("code不能为空");
        }

        // 调用微信接口获取openid
        String openid = getOpenidFromWx(code);
        if (openid == null) {
            return Result.error("微信登录失败，无法获取openid");
        }

        // 查找是否已有用户绑定了该openid
        User user = userService.getOne(new LambdaQueryWrapper<User>().eq(User::getOpenid, openid));
        if (user != null) {
            // 已绑定，直接登录
            if (user.getStatus() != null && user.getStatus() == 0) {
                return Result.error("账号已被禁用");
            }
            if (user.getRegisterStatus() != null && user.getRegisterStatus() != 1) {
                return Result.error("账号未通过审核");
            }

            String token = jwtTokenUtil.generateToken(user.getId(), user.getUsername());
            Map<String, Object> userInfo = buildUserInfo(user);
            Map<String, Object> result = new HashMap<>();
            result.put("token", token);
            result.put("userInfo", userInfo);
            return Result.success(result);
        }

        // 未绑定，返回openid让前端引导用户绑定已有账号
        Map<String, Object> result = new HashMap<>();
        result.put("needBind", true);
        result.put("openid", openid);
        return Result.success(result);
    }

    /**
     * 微信绑定登录：用户输入已有账号密码 + openid，绑定后登录
     */
    @PostMapping("/wx-bind")
    public Result<?> wxBind(@RequestBody Map<String, String> req) {
        String openid = req.get("openid");
        String username = req.get("username");
        String password = req.get("password");

        if (openid == null || openid.trim().isEmpty()) {
            return Result.error("openid不能为空");
        }
        if (username == null || username.trim().isEmpty()) {
            return Result.error("账号不能为空");
        }
        if (password == null || password.trim().isEmpty()) {
            return Result.error("密码不能为空");
        }

        User user = userService.findByUsername(username);
        if (user == null) {
            return Result.error("用户不存在");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            return Result.error("账号已被禁用");
        }
        if (user.getRegisterStatus() != null && user.getRegisterStatus() != 1) {
            return Result.error("账号未通过审核");
        }

        boolean ok = PasswordUtil.matches(password, user.getSalt(), user.getPassword());
        if (!ok) {
            return Result.error("密码错误");
        }

        // 绑定openid
        user.setOpenid(openid);
        userService.updateById(user);

        String token = jwtTokenUtil.generateToken(user.getId(), user.getUsername());
        Map<String, Object> userInfo = buildUserInfo(user);
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userInfo", userInfo);
        return Result.success(result);
    }

    private Map<String, Object> buildUserInfo(User user) {
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("employeeNo", user.getEmployeeNo());
        userInfo.put("name", user.getName());
        userInfo.put("departmentId", user.getDepartmentId());
        userInfo.put("departmentName", user.getDepartmentName());
        userInfo.put("position", user.getPosition());
        userInfo.put("phone", user.getPhone());
        userInfo.put("roleType", user.getRoleType());
        return userInfo;
    }

    private String getOpenidFromWx(String code) {
        try {
            String urlStr = "https://api.weixin.qq.com/sns/jscode2session?appid=" + wxAppId
                    + "&secret=" + wxSecret + "&js_code=" + code + "&grant_type=authorization_code";
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();

            ObjectMapper mapper = new ObjectMapper();
            JsonNode json = mapper.readTree(sb.toString());
            if (json.has("openid")) {
                return json.get("openid").asText();
            }
            System.out.println("微信返回: " + sb.toString());
            return null;
        } catch (Exception e) {
            System.out.println("获取openid异常: " + e.getMessage());
            return null;
        }
    }
}
