package com.myself.teamfiles.module.file.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("file_info")
public class FileInfo {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String fileName;
    private String originalName;
    private String filePath;
    private Long fileSize;
    private String fileType;
    private String mimeType;
    private String fileExtension;
    private String md5;
    private String storageSpace;
    private Long folderId;
    private Long uploadUserId;
    private Integer downloadCount;
    private Integer previewCount;
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
