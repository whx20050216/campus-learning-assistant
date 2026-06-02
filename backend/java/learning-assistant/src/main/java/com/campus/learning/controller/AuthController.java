package com.campus.learning.controller;

import com.campus.learning.dto.LoginDTO;
import com.campus.learning.dto.RefreshTokenDTO;
import com.campus.learning.dto.RegisterDTO;
import com.campus.learning.dto.ResetPasswordDTO;
import com.campus.learning.dto.Result;
import com.campus.learning.dto.UpdateProfileDTO;
import com.campus.learning.entity.User;
import com.campus.learning.security.CurrentUserUtils;
import com.campus.learning.mapper.UserMapper;
import com.campus.learning.security.JwtUtils;
import com.campus.learning.vo.TokenVO;
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
    public Result<TokenVO> login(@RequestBody LoginDTO dto) {
        if (dto.getAccount() == null || dto.getAccount().trim().isEmpty()) {
            return Result.error("账号不能为空");
        }
        if (dto.getPassword() == null || dto.getPassword().isEmpty()) {
            return Result.error("密码不能为空");
        }

        // 登录失败锁定检查
        String failKey = "login:fail:" + dto.getAccount();
        String failCountStr = redisTemplate.opsForValue().get(failKey);
        if (failCountStr != null && Integer.parseInt(failCountStr) >= 5) {
            return Result.error("账号或密码错误，该账号已锁定30分钟");
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
            return Result.error("账号或密码错误");
        }

        if (user.getStatus() == null || user.getStatus() == 0) {
            return Result.error("账号已被冻结");
        }

        if (!encoder.matches(dto.getPassword(), user.getPassword())) {
            Long count = redisTemplate.opsForValue().increment(failKey, 1);
            if (count != null && count == 1) {
                redisTemplate.expire(failKey, 30, TimeUnit.MINUTES);
            }
            if (count != null && count >= 5) {
                return Result.error("账号或密码错误，该账号已锁定30分钟");
            }
            return Result.error("账号或密码错误");
        }

        // 登录成功，清除失败计数
        redisTemplate.delete(failKey);

        boolean rememberMe = Boolean.TRUE.equals(dto.getRememberMe());
        long accessTokenExpireMs = rememberMe ? 604800000L : 7200000L;

        String accessToken = jwtUtils.generateToken(user.getId(), user.getRole(), accessTokenExpireMs);
        String refreshToken = jwtUtils.generateRefreshToken(user.getId());

        redisTemplate.opsForValue().set(
                "token:" + user.getId(),
                accessToken,
                accessTokenExpireMs,
                TimeUnit.MILLISECONDS
        );

        redisTemplate.opsForValue().set(
                "refresh:" + user.getId(),
                refreshToken,
                604800000L,
                TimeUnit.MILLISECONDS
        );

        TokenVO vo = new TokenVO();
        vo.setAccessToken(accessToken);
        vo.setRefreshToken(refreshToken);
        return Result.success(vo);
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

    @PostMapping("/refresh")
    public Result<TokenVO> refresh(@RequestBody RefreshTokenDTO dto) {
        if (dto.getRefreshToken() == null || dto.getRefreshToken().isEmpty()) {
            return Result.error("refreshToken 不能为空");
        }

        if (!jwtUtils.validateRefreshToken(dto.getRefreshToken())) {
            return Result.error("refreshToken 无效或已过期");
        }

        Long userId = jwtUtils.getUserId(dto.getRefreshToken());
        String storedToken = redisTemplate.opsForValue().get("refresh:" + userId);
        if (storedToken == null || !storedToken.equals(dto.getRefreshToken())) {
            return Result.error("refreshToken 无效");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }

        String newAccessToken = jwtUtils.generateToken(user.getId(), user.getRole(), 7200000L);
        String newRefreshToken = jwtUtils.generateRefreshToken(user.getId());

        redisTemplate.opsForValue().set(
                "token:" + userId,
                newAccessToken,
                7200000L,
                TimeUnit.MILLISECONDS
        );

        redisTemplate.opsForValue().set(
                "refresh:" + userId,
                newRefreshToken,
                604800000L,
                TimeUnit.MILLISECONDS
        );

        TokenVO vo = new TokenVO();
        vo.setAccessToken(newAccessToken);
        vo.setRefreshToken(newRefreshToken);
        return Result.success(vo);
    }

    // @PostMapping("/reset-password")
    // public Result<String> resetPassword(@RequestBody ResetPasswordDTO dto) {
    //     if (dto.getNewPassword() == null || dto.getNewPassword().isEmpty()) {
    //         return Result.error("新密码不能为空");
    //     }
    //
    //     User user = null;
    //     if (dto.getStudentNo() != null && !dto.getStudentNo().isEmpty()) {
    //         QueryWrapper<User> wrapper = new QueryWrapper<>();
    //         wrapper.eq("student_no", dto.getStudentNo());
    //         user = userMapper.selectOne(wrapper);
    //     } else if (dto.getEmail() != null && !dto.getEmail().isEmpty()) {
    //         QueryWrapper<User> wrapper = new QueryWrapper<>();
    //         wrapper.eq("email", dto.getEmail());
    //         user = userMapper.selectOne(wrapper);
    //     } else {
    //         return Result.error("学号或邮箱至少提供一个");
    //     }
    //
    //     if (user == null) {
    //         return Result.error("用户不存在");
    //     }
    //
    //     user.setPassword(encoder.encode(dto.getNewPassword()));
    //     user.setUpdatedAt(LocalDateTime.now());
    //     userMapper.updateById(user);
    //
    //     return Result.success("密码重置成功");
    // }

    @PutMapping("/profile")
    public Result<UserVO> updateProfile(@RequestBody UpdateProfileDTO dto) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        if (userId == null) {
            return Result.error(401, "未登录");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.error(404, "用户不存在");
        }

        // 只允许更新邮箱、专业、年级
        if (dto.getEmail() != null && !dto.getEmail().trim().isEmpty()) {
            user.setEmail(dto.getEmail().trim());
        }
        if (dto.getMajor() != null) {
            user.setMajor(dto.getMajor().trim());
        }
        if (dto.getGrade() != null) {
            user.setGrade(dto.getGrade().trim());
        }

        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);

        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        return Result.success(vo);
    }
}
