package com.myself.teamfiles.common.util;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class ZipUtil {

    private ZipUtil() {}

    public static void createZip(List<PathEntry> entries, OutputStream outputStream) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(outputStream)) {
            for (PathEntry entry : entries) {
                addToZip(zos, entry.path(), entry.entryName());
            }
        }
    }

    private static void addToZip(ZipOutputStream zos, Path filePath, String entryName) throws IOException {
        ZipEntry zipEntry = new ZipEntry(entryName);
        zos.putNextEntry(zipEntry);
        Files.copy(filePath, zos);
        zos.closeEntry();
    }

    public record PathEntry(Path path, String entryName) {}
}
