package com.campus.learning;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.campus.learning.mapper")  // 添加这行
public class LearningAssistantApplication {
    public static void main(String[] args) {
        SpringApplication.run(LearningAssistantApplication.class, args);
    }
}