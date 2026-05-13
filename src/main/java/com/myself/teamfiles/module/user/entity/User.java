package com.myself.teamfiles.module.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;
    private String password;
    private String nickname;
    private String avatar;
    private String role;
    private Integer status;
    private Integer firstLogin;
    private Integer loginFailCount;
    private LocalDateTime lockedUntil;
    private Long storageQuota;
    private String remark;
    private Long createdBy;
    private LocalDateTime lastLoginTime;
    private String lastLoginIp;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
