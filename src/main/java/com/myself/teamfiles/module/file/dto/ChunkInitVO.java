package com.myself.teamfiles.module.file.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChunkInitVO {
    private String uploadId;
    private Integer chunkCount;
    private Long chunkSize;
    private boolean skipUpload; // MD5 matched, can skip
}
