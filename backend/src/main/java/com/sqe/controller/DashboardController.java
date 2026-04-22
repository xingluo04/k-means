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

    /* 获取统计概览 */
    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        return Result.success(dashboardService.getOverview());
    }

    /* 获取各维度平均分 */
    @GetMapping("/dimension-avg")
    public Result<Map<String, Object>> dimensionAvg(@RequestParam(defaultValue = "2024-2") String semester) {
        return Result.success(dashboardService.getDimensionAvg(semester));
    }

    /* 获取聚类分布 */
    @GetMapping("/cluster-distribution")
    public Result<List<Map<String, Object>>> clusterDistribution(@RequestParam(defaultValue = "2024-2") String semester) {
        return Result.success(dashboardService.getClusterDistribution(semester));
    }

    /* 获取成绩分布 */
    @GetMapping("/score-distribution")
    public Result<Map<String, Object>> scoreDistribution(@RequestParam(defaultValue = "2024-2") String semester) {
        return Result.success(dashboardService.getScoreDistribution(semester));
    }
}
