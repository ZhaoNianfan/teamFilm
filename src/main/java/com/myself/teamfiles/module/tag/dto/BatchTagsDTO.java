package com.myself.teamfiles.module.tag.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class BatchTagsDTO {
    @NotNull(message = "File IDs cannot be null")
    private List<Long> fileIds;

    @NotNull(message = "Tag IDs cannot be null")
    private List<Long> tagIds;

    // true = add tags, false = remove tags
    private boolean add = true;
}
