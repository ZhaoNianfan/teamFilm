package com.myself.teamfiles.common.enums;

import lombok.Getter;

@Getter
public enum UserRoleEnum {
    ADMIN("ROLE_ADMIN", "Administrator"),
    FORMAL("ROLE_FORMAL", "Formal User"),
    GUEST("ROLE_GUEST", "Guest User");

    private final String springRole;
    private final String description;

    UserRoleEnum(String springRole, String description) {
        this.springRole = springRole;
        this.description = description;
    }
}
