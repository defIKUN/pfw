package com.river.detection.controller;

import com.river.detection.common.Result;
import com.river.detection.entity.User;
import com.river.detection.service.AuthService;
import com.river.detection.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserService userService;

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> loginRequest) {
        try {
            String username = loginRequest.get("username");
            String password = loginRequest.get("password");
            Map<String, Object> result = authService.login(username, password);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/logout")
    public Result<String> logout() {
        return Result.success("登出成功");
    }

    // 注册：创建用户，默认角色 user
    @PostMapping("/register")
    public Result<String> register(@RequestBody Map<String, String> body) {
        try {
            String username = body.get("username");
            String password = body.get("password");
            if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) {
                return Result.error("用户名或密码不能为空");
            }
            if (userService.findByUsername(username) != null) {
                return Result.error("用户名已存在");
            }
            User user = new User();
            user.setUsername(username.trim());
            user.setPassword(password);
            user.setRole("user");
            userService.createUser(user);
            return Result.success("注册成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    // 找回密码：根据用户名将密码重置为 admin123
    @PostMapping("/forgot")
    public Result<String> forgot(@RequestBody Map<String, String> body) {
        try {
            String username = body.get("username");
            if (username == null || username.trim().isEmpty()) {
                return Result.error("请输入用户名");
            }
            String uname = username.trim();
            if (userService.findByUsername(uname) == null) {
                return Result.error("用户不存在");
            }
            userService.resetPassword(uname, "admin123");
            return Result.success("密码已重置为 admin123，请尽快登录并修改密码");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
