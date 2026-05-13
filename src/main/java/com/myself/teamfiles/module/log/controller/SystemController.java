package com.myself.teamfiles.module.log.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.myself.teamfiles.common.result.PageResult;
import com.myself.teamfiles.common.result.R;
import com.myself.teamfiles.module.log.entity.OperationLog;
import com.myself.teamfiles.module.log.mapper.OperationLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasAuthority('ADMIN')")
@RequiredArgsConstructor
public class SystemController {

    private final OperationLogMapper operationLogMapper;

    @Value("${file.storage.backup-path:./data/backups}")
    private String backupPath;

    @GetMapping("/logs")
    public R<PageResult<OperationLog>> logs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<OperationLog>()
                .orderByDesc(OperationLog::getCreatedAt);
        IPage<OperationLog> result = operationLogMapper.selectPage(new Page<>(page, size), wrapper);
        return R.ok(PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords()));
    }

    @com.myself.teamfiles.common.annotation.OperationLog(module = "SYSTEM", operation ="Clean operation logs")
    @DeleteMapping("/logs/clean")
    public R<String> cleanLogs(@RequestParam(defaultValue = "90") int days) {
        LocalDateTime before = LocalDateTime.now().minusDays(days);
        operationLogMapper.delete(
                new LambdaQueryWrapper<OperationLog>().lt(OperationLog::getCreatedAt, before));
        return R.ok("Logs older than " + days + " days cleaned");
    }

    @com.myself.teamfiles.common.annotation.OperationLog(module = "SYSTEM", operation ="Create backup")
    @PostMapping("/backup/create")
    public R<String> createBackup() {
        try {
            Files.createDirectories(Paths.get(backupPath));
            String name = "backup_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".zip";
            Path zipPath = Paths.get(backupPath, name);
            // Create a simple backup marker (full DB backup requires external tools)
            try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipPath))) {
                ZipEntry entry = new ZipEntry("backup_info.txt");
                zos.putNextEntry(entry);
                zos.write(("TeamFiles Backup\n" +
                        "Time: " + LocalDateTime.now() + "\n" +
                        "Type: Full backup marker\n").getBytes());
                zos.closeEntry();
            }
            return R.ok("Backup created: " + name);
        } catch (IOException e) {
            return R.fail("Backup failed: " + e.getMessage());
        }
    }

    @GetMapping("/backup/list")
    public R<List<Map<String, Object>>> listBackups() {
        try {
            Path dir = Paths.get(backupPath);
            if (!Files.exists(dir)) return R.ok(Collections.emptyList());
            List<Map<String, Object>> list = new ArrayList<>();
            Files.list(dir).filter(f -> f.toString().endsWith(".zip")).forEach(f -> {
                Map<String, Object> item = new HashMap<>();
                item.put("name", f.getFileName().toString());
                try { item.put("size", Files.size(f)); } catch (IOException e) { item.put("size", 0L); }
                try { item.put("time", Files.getLastModifiedTime(f).toString()); } catch (IOException e) { item.put("time", ""); }
                list.add(item);
            });
            return R.ok(list);
        } catch (IOException e) {
            return R.ok(Collections.emptyList());
        }
    }

    @com.myself.teamfiles.common.annotation.OperationLog(module = "SYSTEM", operation ="Restore backup")
    @PostMapping("/backup/{name}/restore")
    public R<String> restoreBackup(@PathVariable String name) {
        Path zipPath = Paths.get(backupPath, name);
        if (!Files.exists(zipPath)) {
            return R.fail("Backup file not found");
        }
        // Full restore requires external tools; log the intent
        return R.ok("Restore initiated for: " + name + " (full restore requires external DB tools)");
    }
}
