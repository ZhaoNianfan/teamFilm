package com.myself.teamfiles.module.recycle.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RecycleVO {
    private Long id;
    private String originalType;
    private Long originalId;
    private String fileName;
    private Long fileSize;
    private String storageSpace;
    private Long originalParentId;
    private String originalParentName;
    private Long deletedBy;
    private String deletedByName;
    private LocalDateTime deletedAt;
    private LocalDateTime expireAt;

    public String getDisplaySize() {
        if (fileSize == null) return "0 B";
        long size = fileSize;
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.1f MB", size / (1024.0 * 1024));
        return String.format("%.1f GB", size / (1024.0 * 1024 * 1024));
    }

    public long getRemainingDays() {
        if (expireAt == null) return 0;
        long days = java.time.Duration.between(LocalDateTime.now(), expireAt).toDays();
        return Math.max(0, days);
    }
}
