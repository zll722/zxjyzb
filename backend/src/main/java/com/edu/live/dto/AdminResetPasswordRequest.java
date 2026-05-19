package com.edu.live.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminResetPasswordRequest {
    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 50, message = "新密码长度为6-50个字符")
    private String newPassword;
}
