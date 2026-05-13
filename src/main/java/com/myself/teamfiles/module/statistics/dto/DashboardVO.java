package com.myself.teamfiles.module.statistics.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class DashboardVO {
    private StorageInfo storage;
    private long totalFiles;
    private long totalFolders;
    private List<FileTypeStat> fileTypeDistribution;
    private List<RecentFile> recentFiles;
    private List<TagStat> hotTags;
    private long todayUploads;

    @Data
    public static class StorageInfo {
        private long used;
        private long quota;
        private double percentage;
    }

    @Data
    public static class FileTypeStat {
        private String type;
        private long count;
        private long totalSize;
    }

    @Data
    public static class RecentFile {
        private Long id;
        private String name;
        private String type;
        private String uploader;
        private String createdAt;
    }

    @Data
    public static class TagStat {
        private Long id;
        private String name;
        private String color;
        private long count;
    }
}
