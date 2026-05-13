package com.myself.teamfiles.module.tag.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_tag_visible")
public class UserTagVisible {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Long tagId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
