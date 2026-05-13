package com.myself.teamfiles.module.tag.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TagDTO {
    private Long id;

    @NotBlank(message = "Tag name cannot be empty")
    private String tagName;

    private String color = "#409EFF";
    private Integer sortOrder = 0;
}
