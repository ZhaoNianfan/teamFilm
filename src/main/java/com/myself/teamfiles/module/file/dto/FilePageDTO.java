package com.myself.teamfiles.module.file.dto;

import lombok.Data;

@Data
public class FilePageDTO {
    private Integer page = 1;
    private Integer size = 20;
    private String keyword;
    private String fileType;
    private String storageSpace;
    private Long folderId;
    private String orderBy = "created_at";
    private String orderDir = "desc";
}
