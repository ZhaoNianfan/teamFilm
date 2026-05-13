package com.myself.teamfiles.module.tag.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("file_tag")
public class FileTag {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long fileId;
    private Long tagId;
    private Long createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
