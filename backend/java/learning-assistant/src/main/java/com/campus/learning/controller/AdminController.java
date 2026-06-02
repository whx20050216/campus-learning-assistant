package com.campus.learning.controller;

import com.campus.learning.dto.Result;
import com.campus.learning.entity.Material;
import com.campus.learning.entity.StudyPlan;
import com.campus.learning.entity.User;
import com.campus.learning.mapper.MaterialMapper;
import com.campus.learning.mapper.StudyPlanMapper;
import com.campus.learning.mapper.UserMapper;
import com.campus.learning.security.CurrentUserUtils;
import com.campus.learning.vo.SystemStatusVO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.text.DecimalFormat;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private MaterialMapper materialMapper;

    @Autowired
    private StudyPlanMapper studyPlanMapper;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Value("${python.api.url:http://python-app:8000}")
    private String pythonApiUrl;

    private <T> Result<T> checkAdmin() {
        Long userId = CurrentUserUtils.getCurrentUserId();
        User user = userMapper.selectById(userId);
        if (user == null || !"ADMIN".equals(user.getRole())) {
            return Result.error(403, "无权限");
        }
        return null;
    }

    @PostMapping("/users/{userId}/freeze")
    public Result<Void> freezeUser(@PathVariable Long userId, @RequestParam boolean freeze) {
        Result<Void> checkResult = checkAdmin();
        if (checkResult != null) {
            return checkResult;
        }
        if (userId.equals(CurrentUserUtils.getCurrentUserId())) {
            return Result.error(400, "不能冻结自己");
        }
        User target = userMapper.selectById(userId);
        if (target == null) {
            return Result.error(404, "用户不存在");
        }
        target.setStatus(freeze ? 0 : 1);
        userMapper.updateById(target);
        return Result.success(null);
    }

    @GetMapping("/users")
    public Result<Page<User>> listUsers(@RequestParam(defaultValue = "1") int page,
                                        @RequestParam(defaultValue = "10") int size) {
        Result<Page<User>> checkResult = checkAdmin();
        if (checkResult != null) {
            return checkResult;
        }
        Page<User> pageParam = new Page<>(page, size);
        QueryWrapper<User> wrapper = new QueryWrapper<User>()
                .eq("role", "STUDENT")
                .orderByDesc("created_at");
        Page<User> userPage = userMapper.selectPage(pageParam, wrapper);
        return Result.success(userPage);
    }

    @GetMapping("/materials/pending")
    public Result<Page<Material>> listPendingMaterials(@RequestParam(defaultValue = "1") int page,
                                                       @RequestParam(defaultValue = "10") int size) {
        Result<Page<Material>> checkResult = checkAdmin();
        if (checkResult != null) {
            return checkResult;
        }
        Page<Material> pageParam = new Page<>(page, size);
        QueryWrapper<Material> wrapper = new QueryWrapper<Material>()
                .eq("audit_status", "pending")
                .orderByDesc("created_at");
        Page<Material> materialPage = materialMapper.selectPage(pageParam, wrapper);
        return Result.success(materialPage);
    }

    @PostMapping("/materials/{materialId}/audit")
    public Result<Void> auditMaterial(@PathVariable Long materialId, @RequestParam String action) {
        Result<Void> checkResult = checkAdmin();
        if (checkResult != null) {
            return checkResult;
        }
        Material material = materialMapper.selectById(materialId);
        if (material == null) {
            return Result.error(404, "资料不存在");
        }
        UpdateWrapper<Material> wrapper = new UpdateWrapper<>();
        wrapper.eq("id", materialId);
        if ("approve".equals(action)) {
            wrapper.set("audit_status", "approved");
        } else if ("reject".equals(action)) {
            wrapper.set("audit_status", "rejected");
            wrapper.set("deleted_at", java.time.LocalDateTime.now());
            wrapper.set("deleted_by", "admin");
        } else {
            return Result.error(400, "action 参数必须为 approve 或 reject");
        }
        materialMapper.update(null, wrapper);
        return Result.success(null);
    }

    @GetMapping("/status")
    public Result<SystemStatusVO> getStatus() {
        Result<SystemStatusVO> checkResult = checkAdmin();
        if (checkResult != null) {
            return checkResult;
        }
        SystemStatusVO vo = new SystemStatusVO();
        vo.setTotalUsers(userMapper.selectCount(null));
        vo.setTodayUploads(materialMapper.selectCount(new QueryWrapper<Material>().apply("DATE(created_at) = CURDATE()")));
        vo.setActivePlans(studyPlanMapper.selectCount(new QueryWrapper<StudyPlan>().eq("status", "active")));

        // 1. Java 状态：通过 JVM 内存使用率探测
        try {
            MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
            MemoryUsage heapUsage = memoryMXBean.getHeapMemoryUsage();
            long used = heapUsage.getUsed();
            long max = heapUsage.getMax();
            double usagePercent = max > 0 ? (double) used / max * 100 : 0;
            if (usagePercent > 90) {
                vo.setJavaStatus("WARNING");
            } else {
                vo.setJavaStatus("UP");
            }
        } catch (Exception e) {
            vo.setJavaStatus("DOWN");
        }

        // 2. Python 状态
        try {
            restTemplate.getForObject(pythonApiUrl + "/health", String.class);
            vo.setPythonStatus("UP");
        } catch (Exception e) {
            vo.setPythonStatus("DOWN");
        }

        // 3. DB 状态：执行 SELECT 1
        try {
            userMapper.selectCount(new QueryWrapper<User>().last("LIMIT 1"));
            vo.setDbStatus("UP");
        } catch (Exception e) {
            vo.setDbStatus("DOWN");
        }

        // 4. Redis 状态：执行 PING
        try {
            String pingResult = redisTemplate.execute((org.springframework.data.redis.core.RedisCallback<String>) connection -> {
                return new String(connection.ping());
            });
            if ("PONG".equalsIgnoreCase(pingResult)) {
                vo.setRedisStatus("UP");
            } else {
                vo.setRedisStatus("DOWN");
            }
        } catch (Exception e) {
            vo.setRedisStatus("DOWN");
        }

        // 5. 磁盘空间：监控根目录 /
        try {
            File root = new File("/");
            long totalSpace = root.getTotalSpace();
            long usableSpace = root.getUsableSpace();
            long usedSpace = totalSpace - usableSpace;
            double usagePercent = totalSpace > 0 ? (double) usedSpace / totalSpace * 100 : 0;
            DecimalFormat df = new DecimalFormat("0.0");
            vo.setDiskUsage(df.format(usagePercent) + "%");
            if (usagePercent > 90) {
                vo.setDiskStatus("CRITICAL");
            } else if (usagePercent > 80) {
                vo.setDiskStatus("WARNING");
            } else {
                vo.setDiskStatus("UP");
            }
        } catch (Exception e) {
            vo.setDiskStatus("DOWN");
            vo.setDiskUsage("未知");
        }

        return Result.success(vo);
    }
}
