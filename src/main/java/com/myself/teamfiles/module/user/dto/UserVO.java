package com.myself.teamfiles.module.user.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserVO {
    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private String role;
    private Integer status;
    private Integer firstLogin;
    private Long storageQuota;
    private String remark;
    private Long createdBy;
    private LocalDateTime lastLoginTime;
    private String lastLoginIp;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
