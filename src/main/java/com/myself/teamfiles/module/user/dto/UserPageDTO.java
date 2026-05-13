package com.myself.teamfiles.module.user.dto;

import lombok.Data;

@Data
public class UserPageDTO {
    private String keyword;
    private String role;
    private Integer status;
    private Integer page = 1;
    private Integer pageSize = 20;
}
