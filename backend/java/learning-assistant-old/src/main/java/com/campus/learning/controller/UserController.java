package com.campus.learning.controller;

import com.campus.learning.entity.User;
import com.campus.learning.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public String register(@RequestParam String username,
                           @RequestParam String email,
                           @RequestParam String password) {
        try {
            User user = userService.register(username, email, password);
            return "注册成功，用户ID：" + user.getId();
        } catch (Exception e) {
            return "注册失败：" + e.getMessage();
        }
    }

    @GetMapping("/test")
    public String test() {
        return "用户模块运行正常";
    }
}