package com.myself.teamfiles.module.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_api_config")
public class AiApiConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Integer isSystem;
    private String apiType;
    private String apiKey;
    private String apiBaseUrl;
    private String modelName;
    private Integer isActive;
    private Integer tested;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
