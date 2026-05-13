package com.myself.teamfiles.module.search.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("search_history")
public class SearchHistory {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String keyword;
    private String searchType;
    private Integer resultCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
