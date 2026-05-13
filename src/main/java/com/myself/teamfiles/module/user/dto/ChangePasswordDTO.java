package com.myself.teamfiles.module.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChangePasswordDTO {
    private String username;
    private String oldPassword;

    @NotBlank(message = "New password is required")
    private String newPassword;
}
