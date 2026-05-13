package com.myself.teamfiles.module.search.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("search_template")
public class SearchTemplate {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String templateName;
    private String searchCondition;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
