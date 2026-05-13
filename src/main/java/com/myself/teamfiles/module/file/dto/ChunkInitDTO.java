package com.myself.teamfiles.module.file.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChunkInitDTO {
    @NotBlank(message = "File name cannot be empty")
    private String fileName;

    @NotNull(message = "File size cannot be null")
    private Long fileSize;

    private String fileMd5;
    private String storageSpace;
    private Long folderId;
}
