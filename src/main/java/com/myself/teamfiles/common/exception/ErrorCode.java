package com.myself.teamfiles.common.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {
    SUCCESS(200, "Success"),

    // 4xx Auth
    UNAUTHORIZED(401, "Unauthorized"),
    TOKEN_EXPIRED(402, "Token expired"),
    FORBIDDEN(403, "Access denied"),
    NOT_FOUND(404, "Resource not found"),

    // 4xx Business
    BAD_REQUEST(400, "Bad request"),
    USERNAME_EXISTS(1001, "Username already exists"),
    USER_NOT_FOUND(1002, "User not found"),
    USER_DISABLED(1003, "Account is disabled"),
    USER_LOCKED(1004, "Account is locked, please try again later"),
    PASSWORD_ERROR(1005, "Incorrect password"),
    FIRST_LOGIN_MUST_CHANGE_PASSWORD(1006, "First login, please change password"),
    FILE_NOT_FOUND(2001, "File not found"),
    FILE_UPLOAD_FAILED(2002, "File upload failed"),
    FILE_SIZE_EXCEEDED(2003, "File size exceeds limit"),
    FILE_TYPE_NOT_SUPPORTED(2004, "File type not supported"),
    FOLDER_NOT_FOUND(2101, "Folder not found"),
    TAG_NOT_FOUND(3001, "Tag not found"),
    TAG_NAME_EXISTS(3002, "Tag name already exists"),
    STORAGE_QUOTA_EXCEEDED(4001, "Storage quota exceeded"),
    AI_CONFIG_FAILED(5001, "AI configuration failed"),
    AI_API_ERROR(5002, "AI API call failed"),

    // 5xx
    INTERNAL_ERROR(500, "Internal server error");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
