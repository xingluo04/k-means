package com.sqe.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 注册请求参数
 */
@Data
public class RegisterDTO {
    @NotBlank(message = "用户名不能为空")
    private String username;
    @NotBlank(message = "密码不能为空")
    private String password;
    @NotBlank(message = "真实姓名不能为空")
    private String realName;
    @NotBlank(message = "角色不能为空")
    private String role;
    private String phone;
    private String email;
    private Integer gender;
}
