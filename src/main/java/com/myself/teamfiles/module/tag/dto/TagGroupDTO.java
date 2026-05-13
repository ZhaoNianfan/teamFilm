package com.myself.teamfiles.module.tag.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class TagGroupDTO {
    private Long id;

    @NotBlank(message = "Group name cannot be empty")
    private String groupName;

    private String color = "#409EFF";
    private Integer sortOrder = 0;

    // For response
    private List<TagVO> tags;
    private Integer tagCount;

    // For adding/removing tags
    private List<Long> tagIds;
}
