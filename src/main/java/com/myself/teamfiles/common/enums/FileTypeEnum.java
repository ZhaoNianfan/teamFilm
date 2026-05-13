package com.myself.teamfiles.common.enums;

import lombok.Getter;

@Getter
public enum FileTypeEnum {
    IMAGE("Image"),
    DOCUMENT("Document"),
    OTHER("Other");

    private final String description;

    FileTypeEnum(String description) {
        this.description = description;
    }
}
