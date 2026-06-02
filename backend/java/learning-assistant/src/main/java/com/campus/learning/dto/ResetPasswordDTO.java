package com.campus.learning.dto;

import lombok.Data;

@Data
public class ResetPasswordDTO {
    private String studentNo;
    private String email;
    private String newPassword;
}
