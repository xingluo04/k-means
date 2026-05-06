package com.sqe.controller;

import com.sqe.common.Result;
import com.sqe.service.ExcelImportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 数据导入控制器
 */
@RestController
@RequestMapping("/api/import")
public class ImportController {

    @Autowired
    private ExcelImportService excelImportService;

    /**
     * 上传Excel文件导入综测数据
     */
    @PostMapping("/excel")
    public Result<Map<String, Object>> importExcel(@RequestParam("file") MultipartFile file,
                                                    @RequestParam String academicYear) {
        try {
            Map<String, Object> result = excelImportService.importExcel(file, academicYear);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("导入失败：" + e.getMessage());
        }
    }
}
