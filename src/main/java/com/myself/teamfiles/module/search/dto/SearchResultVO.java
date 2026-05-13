package com.myself.teamfiles.module.search.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class SearchResultVO {
    private Long fileId;
    private String originalName;
    private String fileName;
    private String fileType;
    private String fileExtension;
    private Long fileSize;
    private String storageSpace;
    private Long uploadUserId;
    private String uploadUsername;
    private String createdAt;

    // ES highlight
    private Map<String, List<String>> highlights;

    // Relevance score
    private Float score;

    public String getDisplaySize() {
        if (fileSize == null) return "0 B";
        long size = fileSize;
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.1f MB", size / (1024.0 * 1024));
        return String.format("%.1f GB", size / (1024.0 * 1024 * 1024));
    }
}
