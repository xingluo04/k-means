package com.sqe.controller;

import com.sqe.common.Result;
import com.sqe.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 数据看板控制器
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        return Result.success(dashboardService.getOverview());
    }

    @GetMapping("/dimension-avg")
    public Result<Map<String, Object>> dimensionAvg(@RequestParam(defaultValue = "2023-2024") String academicYear) {
        return Result.success(dashboardService.getDimensionAvg(academicYear));
    }

    @GetMapping("/cluster-distribution")
    public Result<List<Map<String, Object>>> clusterDistribution(@RequestParam(defaultValue = "2023-2024") String academicYear) {
        return Result.success(dashboardService.getClusterDistribution(academicYear));
    }

    @GetMapping("/score-distribution")
    public Result<Map<String, Object>> scoreDistribution(@RequestParam(defaultValue = "2023-2024") String academicYear) {
        return Result.success(dashboardService.getScoreDistribution(academicYear));
    }

    @GetMapping("/class-comparison")
    public Result<Map<String, Object>> classComparison(@RequestParam(defaultValue = "2024-2025") String academicYear) {
        return Result.success(dashboardService.getClassComparison(academicYear));
    }
}
