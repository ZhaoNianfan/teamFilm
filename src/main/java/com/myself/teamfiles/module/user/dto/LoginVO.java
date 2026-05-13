package com.myself.teamfiles.module.user.dto;

import lombok.Data;

@Data
public class LoginVO {
    private String token;
    private String tokenType = "Bearer";
    private Long expiresIn;
    private UserVO userInfo;
    private Boolean needChangePassword;
}
