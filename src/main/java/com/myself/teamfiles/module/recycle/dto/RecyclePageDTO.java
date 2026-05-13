package com.myself.teamfiles.module.recycle.dto;

import lombok.Data;

@Data
public class RecyclePageDTO {
    private Integer page = 1;
    private Integer size = 20;
    private String storageSpace;
    private String keyword;
}
