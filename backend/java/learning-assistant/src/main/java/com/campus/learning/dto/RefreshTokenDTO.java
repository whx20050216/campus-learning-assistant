package com.campus.learning.dto;

import lombok.Data;

@Data
public class RefreshTokenDTO {
    private String refreshToken;
    private Boolean rememberMe = false;
}
