package com.myself.teamfiles.common.enums;

import lombok.Getter;

@Getter
public enum StorageSpaceEnum {
    PERSONAL("Personal Space"),
    TEAM("Team Shared Space");

    private final String description;

    StorageSpaceEnum(String description) {
        this.description = description;
    }
}
