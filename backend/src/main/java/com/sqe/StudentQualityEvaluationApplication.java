package com.sqe;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 学生综合素质评价系统启动类
 */
@SpringBootApplication
@MapperScan("com.sqe.mapper")
public class StudentQualityEvaluationApplication {
    public static void main(String[] args) {
        SpringApplication.run(StudentQualityEvaluationApplication.class, args);
    }
}
