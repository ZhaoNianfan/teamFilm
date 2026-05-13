package com.myself.teamfiles.module.file.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class FolderDTO {
    private Long id;

    @NotBlank(message = "Folder name cannot be empty")
    private String folderName;

    private Long parentId = 0L;

    @NotBlank(message = "Storage space cannot be empty")
    private String storageSpace;

    private Integer sortOrder = 0;
    private Long ownerUserId;
    private LocalDateTime createdAt;

    // for tree response
    private List<FolderDTO> children;
    private Integer fileCount;

    // for batch delete
    private List<Long> ids;
}
