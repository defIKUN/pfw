package com.river.detection.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.river.detection.common.Result;
import com.river.detection.entity.User;
import com.river.detection.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@CrossOrigin
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping
    public Result<Page<User>> getUserList(@RequestParam(defaultValue = "1") Integer page,
                                           @RequestParam(defaultValue = "10") Integer size,
                                           @RequestParam(required = false) String keyword) {
        Page<User> userPage = userService.getUserList(page, size, keyword);
        return Result.success(userPage);
    }

    @GetMapping("/{id}")
    public Result<User> getUserById(@PathVariable Long id) {
        User user = userService.getUserById(id);
        return Result.success(user);
    }

    @PostMapping
    public Result<String> createUser(@RequestBody User user) {
        try {
            userService.createUser(user);
            return Result.success("用户创建成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public Result<String> updateUser(@PathVariable Long id, @RequestBody User user) {
        try {
            user.setId(id);
            userService.updateUser(user);
            return Result.success("用户更新成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Result<String> deleteUser(@PathVariable Long id) {
        try {
            userService.deleteUser(id);
            return Result.success("用户删除成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}

