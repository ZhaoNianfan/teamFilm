package com.myself.teamfiles.module.file.dto;

import com.myself.teamfiles.module.tag.dto.TagVO;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Data
public class FileVO {
    private Long id;
    private String fileName;
    private String originalName;
    private Long fileSize;
    private String fileType;
    private String mimeType;
    private String fileExtension;
    private String md5;
    private String storageSpace;
    private Long folderId;
    private String folderName;
    private Long uploadUserId;
    private String uploadUsername;
    private Integer downloadCount;
    private Integer previewCount;
    private boolean shared;
    private List<TagVO> tags = Collections.emptyList();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public String getDisplaySize() {
        if (fileSize == null) return "0 B";
        long size = fileSize;
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.1f MB", size / (1024.0 * 1024));
        return String.format("%.1f GB", size / (1024.0 * 1024 * 1024));
    }
}
