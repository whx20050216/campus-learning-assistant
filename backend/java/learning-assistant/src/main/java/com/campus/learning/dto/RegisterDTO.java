package com.campus.learning.dto;

import lombok.Data;

@Data
public class RegisterDTO {

    private String studentNo;
    private String username;
    private String email;
    private String password;
    private String major;
    private String grade;
}
