package com.myself.teamfiles.module.statistics.dto;

import lombok.Data;

@Data
public class AdminStatsVO {
    private long totalUsers;
    private long activeUsers;
    private long totalFiles;
    private long totalFolders;
    private long teamFiles;
    private long personalFiles;
    private long totalStorageUsed;
    private long todayUploads;
    private long todayDownloads;
    private long recycleItems;
}
