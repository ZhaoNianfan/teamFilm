package com.myself.teamfiles.module.tag.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TagVO {
    private Long id;
    private String tagName;
    private String color;
    private Long creatorUserId;
    private String creatorUsername;
    private Integer sortOrder;
    private Integer fileCount;       // total files with this tag
    private Integer personalFileCount; // files in current user's personal space
    private Integer sharedFileCount;  // files shared to team space
    private LocalDateTime createdAt;
}
