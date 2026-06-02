package com.campus.learning.vo;

import lombok.Data;

@Data
public class SystemStatusVO {
    private Long totalUsers;
    private Long todayUploads;
    private Long activePlans;
    private String javaStatus;
    private String pythonStatus;
    private String dbStatus;
    private String redisStatus;
    private String diskStatus;
    private String diskUsage;
}
