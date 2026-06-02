package com.campus.learning.vo;

import lombok.Data;

import java.time.LocalDate;

@Data
public class PlanReminderVO {

    private Long planId;

    private String planName;

    private LocalDate remindDate;

    private int daysLeft;
}
