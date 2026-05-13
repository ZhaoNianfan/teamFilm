package com.myself.teamfiles.common.constant;

public final class FileConstants {

    private FileConstants() {}

    public static final long MAX_FILE_SIZE = 5L * 1024 * 1024 * 1024; // 5GB
    public static final long DEFAULT_CHUNK_SIZE = 5L * 1024 * 1024;    // 5MB
    public static final int MAX_BATCH_UPLOAD = 20;
    public static final int RECYCLE_RETENTION_DAYS = 30;

    public static final String[] IMAGE_EXTENSIONS = {"jpg", "jpeg", "png", "gif", "bmp", "webp"};
    public static final String[] DOCUMENT_EXTENSIONS = {"doc", "docx", "xls", "xlsx", "ppt", "pptx", "pdf"};
    public static final String[] OTHER_EXTENSIONS = {"txt", "zip", "rar"};
}
