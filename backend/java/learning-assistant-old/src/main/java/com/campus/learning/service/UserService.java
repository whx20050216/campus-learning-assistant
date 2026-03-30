package com.campus.learning.service;

import com.campus.learning.entity.User;

public interface UserService {
    User register(String username, String email, String password);
}