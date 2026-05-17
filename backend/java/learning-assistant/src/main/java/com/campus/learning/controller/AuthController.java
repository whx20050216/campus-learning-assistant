package com.campus.learning.controller;

import com.campus.learning.dto.LoginDTO;
import com.campus.learning.dto.RegisterDTO;
import com.campus.learning.dto.Result;
import com.campus.learning.entity.User;
import com.campus.learning.mapper.UserMapper;
import com.campus.learning.security.JwtUtils;
import com.campus.learning.vo.UserVO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5173")
public class AuthController {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{6,}$");

    @PostMapping("/register")
    public Result<Long> register(@RequestBody RegisterDTO dto) {
        if (dto.getStudentNo() == null || dto.getStudentNo().trim().isEmpty()) {
            return Result.error("学号不能为空");
        }
        if (dto.getUsername() == null || dto.getUsername().trim().isEmpty()) {
            return Result.error("用户名不能为空");
        }
        if (dto.getEmail() == null || dto.getEmail().trim().isEmpty()) {
            return Result.error("邮箱不能为空");
        }
        if (dto.getPassword() == null || !PASSWORD_PATTERN.matcher(dto.getPassword()).matches()) {
            return Result.error("密码至少6位，且必须同时包含字母和数字");
        }

        if (userMapper.existsByStudentNo(dto.getStudentNo())) {
            return Result.error("学号已存在");
        }
        if (userMapper.existsByUsername(dto.getUsername())) {
            return Result.error("用户名已存在");
        }

        QueryWrapper<User> emailWrapper = new QueryWrapper<>();
        emailWrapper.eq("email", dto.getEmail());
        if (userMapper.selectCount(emailWrapper) > 0) {
            return Result.error("邮箱已存在");
        }

        User user = new User();
        user.setStudentNo(dto.getStudentNo());
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setPassword(encoder.encode(dto.getPassword()));
        user.setRole("STUDENT");
        user.setStatus(1);
        user.setMajor(dto.getMajor());
        user.setGrade(dto.getGrade());
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        userMapper.insert(user);
        return Result.success(user.getId());
    }

    @PostMapping("/login")
    public Result<String> login(@RequestBody LoginDTO dto) {
        if (dto.getAccount() == null || dto.getAccount().trim().isEmpty()) {
            return Result.error("账号不能为空");
        }
        if (dto.getPassword() == null || dto.getPassword().isEmpty()) {
            return Result.error("密码不能为空");
        }

        User user = null;

        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("student_no", dto.getAccount());
        user = userMapper.selectOne(wrapper);

        if (user == null) {
            QueryWrapper<User> emailWrapper = new QueryWrapper<>();
            emailWrapper.eq("email", dto.getAccount());
            user = userMapper.selectOne(emailWrapper);
        }

        if (user == null) {
            return Result.error("用户不存在");
        }

        if (user.getStatus() == null || user.getStatus() == 0) {
            return Result.error("账号已被冻结");
        }

        if (!encoder.matches(dto.getPassword(), user.getPassword())) {
            return Result.error("密码错误");
        }

        String token = jwtUtils.generateToken(user.getId(), user.getRole());

        redisTemplate.opsForValue().set(
                "token:" + user.getId(),
                token,
                2,
                TimeUnit.HOURS
        );

        return Result.success(token);
    }

    @GetMapping("/me")
    public Result<UserVO> me(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Result.error(401, "未登录");
        }

        String token = authHeader.substring(7);
        if (!jwtUtils.validateToken(token)) {
            return Result.error(401, "Token 无效或已过期");
        }

        Long userId = jwtUtils.getUserId(token);
        User user = userMapper.selectById(userId);

        if (user == null) {
            return Result.error(401, "用户不存在");
        }

        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        return Result.success(vo);
    }
}
