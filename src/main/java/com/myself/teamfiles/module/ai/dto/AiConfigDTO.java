package com.myself.teamfiles.module.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AiConfigDTO {
    private Long id;

    @NotBlank(message = "API type is required")
    private String apiType;

    @NotBlank(message = "API key is required")
    private String apiKey;

    private String apiBaseUrl;
    private String modelName;
    private Integer isActive = 1;

    // For admin: set as system-wide config
    private boolean systemConfig = false;
}
