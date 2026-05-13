package com.myself.teamfiles.scheduled;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.myself.teamfiles.module.file.entity.FileChunk;
import com.myself.teamfiles.module.file.mapper.FileChunkMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChunkCleanTask {

    private final FileChunkMapper fileChunkMapper;

    @Value("${file.storage.chunk-path:./data/chunks}")
    private String chunkPath;

    @Scheduled(cron = "0 30 2 * * ?") // Daily at 2:30 AM
    public void cleanExpiredChunks() {
        List<FileChunk> expiredChunks = fileChunkMapper.selectList(
                new LambdaQueryWrapper<FileChunk>()
                        .lt(FileChunk::getExpiredAt, LocalDateTime.now()));

        if (expiredChunks.isEmpty()) return;

        Set<String> uploadIds = expiredChunks.stream()
                .map(FileChunk::getUploadId)
                .collect(Collectors.toSet());

        for (String uploadId : uploadIds) {
            // Delete chunk files
            Path chunkDir = Paths.get(chunkPath, uploadId);
            try {
                if (Files.exists(chunkDir)) {
                    try (var files = Files.list(chunkDir)) {
                        files.forEach(f -> {
                            try { Files.deleteIfExists(f); } catch (IOException ignored) {}
                        });
                    }
                    Files.deleteIfExists(chunkDir);
                }
            } catch (IOException e) {
                log.error("Failed to clean chunk directory: {}", uploadId, e);
            }
            // Delete DB records
            fileChunkMapper.delete(
                    new LambdaQueryWrapper<FileChunk>().eq(FileChunk::getUploadId, uploadId));
        }

        log.info("Cleaned {} expired chunk upload tasks", uploadIds.size());
    }
}
