package com.campus.learning.vo;

import lombok.Data;

import java.util.List;

@Data
public class DashboardVO {

    private List<PieItem> courseDistribution;

    private List<ProgressItem> currentProgress;

    private Integer weeklyDuration;

    private Integer lastWeekDuration;

    private Integer materialCount;
}
