package com.myself.teamfiles.module.file.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("folder")
public class Folder {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String folderName;
    private Long parentId;
    private String storageSpace;
    private Long ownerUserId;
    private Integer sortOrder;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
