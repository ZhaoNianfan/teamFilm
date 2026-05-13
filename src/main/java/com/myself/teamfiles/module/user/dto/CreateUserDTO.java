package com.myself.teamfiles.module.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateUserDTO {
    @NotBlank(message = "Username is required")
    @Size(min = 2, max = 50, message = "Username must be 2-50 characters")
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 100, message = "Password must be 6-100 characters")
    private String password;

    @NotBlank(message = "User role is required")
    private String role;

    private String nickname;
    private String remark;
}
