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

    /* 执行聚类分析 */
    @PostMapping("/execute")
    public Result<Map<String, Object>> execute(@RequestParam String semester,
                                                @RequestParam(defaultValue = "5") int clusterCount) {
        try {
            Map<String, Object> result = clusterService.executeCluster(semester, clusterCount);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /* 获取聚类结果 */
    @GetMapping("/results")
    public Result<List<ClusterResult>> results(@RequestParam String semester) {
        return Result.success(clusterService.getClusterResults(semester));
    }
}
