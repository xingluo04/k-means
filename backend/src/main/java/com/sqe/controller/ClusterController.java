package com.sqe.controller;

import com.sqe.common.Result;
import com.sqe.entity.ClusterResult;
import com.sqe.service.ClusterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 聚类分析控制器
 */
@RestController
@RequestMapping("/api/cluster")
public class ClusterController {

    @Autowired
    private ClusterService clusterService;

    @PostMapping("/execute")
    public Result<Map<String, Object>> execute(@RequestParam String academicYear,
                                                @RequestParam(defaultValue = "5") int clusterCount) {
        try {
            Map<String, Object> result = clusterService.executeCluster(academicYear, clusterCount);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/results")
    public Result<List<ClusterResult>> results(@RequestParam String academicYear) {
        return Result.success(clusterService.getClusterResults(academicYear));
    }

    @GetMapping("/optimal-k")
    public Result<Map<String, Object>> optimalK(@RequestParam String academicYear,
                                                 @RequestParam(defaultValue = "2") int minK,
                                                 @RequestParam(defaultValue = "10") int maxK) {
        try {
            Map<String, Object> result = clusterService.findOptimalK(academicYear, minK, maxK);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
