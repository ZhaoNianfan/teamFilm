package com.myself.teamfiles.module.search.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SearchTemplateDTO {
    private Long id;

    @NotBlank(message = "Template name is required")
    private String templateName;

    @NotBlank(message = "Search condition is required")
    private String searchCondition; // JSON string of SearchDTO
}
