package com.campus.learning.service.impl;

import com.campus.learning.entity.User;
import com.campus.learning.repository.UserRepository;
import com.campus.learning.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Override
    public User register(String username, String email, String password) {
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("用户名已存在");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(encoder.encode(password));
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        return userRepository.save(user);
    }
}