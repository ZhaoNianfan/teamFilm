package com.myself.teamfiles.module.search.dto;

import lombok.Data;

import java.util.List;

@Data
public class SearchDTO {
    private String keyword;
    private String fileType;
    private String fileExtension;
    private String storageSpace;
    private List<Long> tagIds;
    private String tagLogic = "AND"; // AND/OR
    private Long minSize;
    private Long maxSize;
    private String startDate;
    private String endDate;
    private Integer page = 1;
    private Integer size = 20;
    private String sortBy = "_score"; // _score/created_at/file_size/file_name
    private String sortDir = "desc";
}
