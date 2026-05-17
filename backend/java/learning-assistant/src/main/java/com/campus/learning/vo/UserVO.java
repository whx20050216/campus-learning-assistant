package com.campus.learning.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserVO {

    private Long id;
    private String studentNo;
    private String username;
    private String email;
    private String major;
    private String grade;
    private String role;
    private Integer status;
    private LocalDateTime createdAt;
}
