package com.myself.teamfiles.common.constant;

public final class UserConstants {

    private UserConstants() {}

    public static final int LOGIN_MAX_FAIL_COUNT = 5;
    public static final int LOGIN_LOCK_MINUTES = 10;
    public static final long DEFAULT_STORAGE_QUOTA = 5L * 1024 * 1024 * 1024; // 5GB
    public static final String DEFAULT_ADMIN_USERNAME = "admin";
}
