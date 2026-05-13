package com.myself.teamfiles.module.recycle.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("recycle_bin")
public class RecycleBin {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String originalType;
    private Long originalId;
    private String fileName;
    private String filePath;
    private Long fileSize;
    private String storageSpace;
    private Long originalParentId;
    private Long deletedBy;
    private LocalDateTime deletedAt;
    private LocalDateTime expireAt;
}
