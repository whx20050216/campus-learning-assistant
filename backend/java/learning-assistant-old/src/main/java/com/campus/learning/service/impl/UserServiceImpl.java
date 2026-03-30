package com.campus.learning.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.learning.entity.User;
import com.campus.learning.mapper.UserMapper;
import com.campus.learning.service.UserService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Override
    public User register(String username, String email, String password) {
        // 检查用户名是否存在
        if (lambdaQuery().eq(User::getUsername, username).count() > 0) {
            throw new RuntimeException("用户名已存在");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(encoder.encode(password));
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        save(user);
        return user;
    }
}