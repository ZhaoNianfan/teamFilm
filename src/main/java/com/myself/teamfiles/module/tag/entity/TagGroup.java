package com.myself.teamfiles.module.tag.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("tag_group")
public class TagGroup {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String groupName;
    private Long ownerUserId;
    private String color;
    private Integer sortOrder;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
