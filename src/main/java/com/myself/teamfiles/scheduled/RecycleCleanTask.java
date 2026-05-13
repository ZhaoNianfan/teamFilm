package com.myself.teamfiles.scheduled;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.myself.teamfiles.module.recycle.entity.RecycleBin;
import com.myself.teamfiles.module.recycle.mapper.RecycleBinMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecycleCleanTask {

    private final RecycleBinMapper recycleBinMapper;

    @Value("${file.storage.base-path}")
    private String basePath;

    @Scheduled(cron = "0 0 2 * * ?") // Daily at 2:00 AM
    public void cleanExpiredItems() {
        List<RecycleBin> expiredItems = recycleBinMapper.selectList(
                new LambdaQueryWrapper<RecycleBin>()
                        .lt(RecycleBin::getExpireAt, LocalDateTime.now())
        );

        for (RecycleBin item : expiredItems) {
            try {
                // Delete physical file
                if (item.getFilePath() != null) {
                    Files.deleteIfExists(Paths.get(basePath, item.getFilePath()));
                }
                recycleBinMapper.deleteById(item.getId());
                log.debug("Permanently deleted expired recycle item: {}", item.getFileName());
            } catch (IOException e) {
                log.error("Failed to delete recycle item file: {}", item.getFilePath(), e);
            }
        }

        if (!expiredItems.isEmpty()) {
            log.info("Cleaned {} expired recycle items", expiredItems.size());
        }
    }
}
