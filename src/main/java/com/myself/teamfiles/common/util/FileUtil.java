package com.myself.teamfiles.common.util;

import com.myself.teamfiles.common.constant.FileConstants;
import com.myself.teamfiles.common.enums.FileTypeEnum;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class FileUtil {

    private static final Set<String> IMAGE_EXTENSIONS = new HashSet<>(Arrays.asList(FileConstants.IMAGE_EXTENSIONS));
    private static final Set<String> DOCUMENT_EXTENSIONS = new HashSet<>(Arrays.asList(FileConstants.DOCUMENT_EXTENSIONS));

    private FileUtil() {}

    public static String generateStoragePath(String storageSpace, String extension) {
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String uuidName = UUID.randomUUID().toString().replace("-", "");
        return storageSpace + "/" + datePath + "/" + uuidName + (extension != null ? "." + extension : "");
    }

    public static String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    public static String getFileType(String extension) {
        if (IMAGE_EXTENSIONS.contains(extension)) return FileTypeEnum.IMAGE.name();
        if (DOCUMENT_EXTENSIONS.contains(extension)) return FileTypeEnum.DOCUMENT.name();
        return FileTypeEnum.OTHER.name();
    }

    public static String getMimeType(String extension) {
        return switch (extension) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "bmp" -> "image/bmp";
            case "webp" -> "image/webp";
            case "pdf" -> "application/pdf";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls" -> "application/vnd.ms-excel";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "ppt" -> "application/vnd.ms-powerpoint";
            case "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case "txt" -> "text/plain";
            case "zip" -> "application/zip";
            case "rar" -> "application/x-rar-compressed";
            default -> "application/octet-stream";
        };
    }

    public static String md5(InputStream inputStream) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                md.update(buffer, 0, bytesRead);
            }
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException | IOException e) {
            throw new RuntimeException("MD5 calculation failed", e);
        }
    }

    public static String md5(Path filePath) {
        try (InputStream is = Files.newInputStream(filePath)) {
            return md5(is);
        } catch (IOException e) {
            throw new RuntimeException("MD5 calculation failed for: " + filePath, e);
        }
    }

    public static void ensureDirectoryExists(Path path) throws IOException {
        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }
    }

    public static void copyToFile(InputStream source, Path target) throws IOException {
        ensureDirectoryExists(target.getParent());
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
    }

    public static String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
