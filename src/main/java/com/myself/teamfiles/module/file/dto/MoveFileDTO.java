package com.myself.teamfiles.module.file.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MoveFileDTO {
    private Long targetFolderId; // null = move to root
}
