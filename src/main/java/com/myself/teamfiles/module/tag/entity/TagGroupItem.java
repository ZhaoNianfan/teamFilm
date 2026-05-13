package com.myself.teamfiles.module.tag.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("tag_group_item")
public class TagGroupItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long groupId;
    private Long tagId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
