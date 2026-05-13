package com.myself.teamfiles.module.file.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("file_chunk")
public class FileChunk {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String uploadId;
    private String fileName;
    private String fileMd5;
    private Integer chunkIndex;
    private Integer chunkCount;
    private Long chunkSize;
    private String chunkPath;
    private Integer status;
    private Long uploadUserId;
    private String storageSpace;
    private Long folderId;
    private LocalDateTime expiredAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
