package com.myself.teamfiles.module.search.service;

import com.myself.teamfiles.module.file.entity.FileInfo;
import com.myself.teamfiles.module.search.document.FileDocument;
import com.myself.teamfiles.module.user.entity.User;
import com.myself.teamfiles.module.user.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class FileIndexSyncService {

    private final ElasticsearchOperations elasticsearchOperations;
    private final UserMapper userMapper;
    private final boolean esAvailable;

    public FileIndexSyncService(@Autowired(required = false) ElasticsearchOperations elasticsearchOperations,
                                UserMapper userMapper) {
        this.elasticsearchOperations = elasticsearchOperations;
        this.userMapper = userMapper;
        this.esAvailable = elasticsearchOperations != null;
    }

    @Async
    public void indexFile(FileInfo file) {
        if (!esAvailable) return;
        try {
            FileDocument doc = toDocument(file);
            elasticsearchOperations.save(doc, IndexCoordinates.of("teamfiles_files"));
            log.debug("Indexed file: {}", file.getId());
        } catch (Exception e) {
            log.warn("Failed to index file {}: {}", file.getId(), e.getMessage());
        }
    }

    @Async
    public void deleteIndex(Long fileId) {
        if (!esAvailable) return;
        try {
            elasticsearchOperations.delete(String.valueOf(fileId), IndexCoordinates.of("teamfiles_files"));
            log.debug("Deleted index for file: {}", fileId);
        } catch (Exception e) {
            log.warn("Failed to delete index for file {}: {}", fileId, e.getMessage());
        }
    }

    @Async
    public void updateIndex(FileInfo file) {
        if (!esAvailable) return;
        try {
            FileDocument doc = toDocument(file);
            elasticsearchOperations.save(doc, IndexCoordinates.of("teamfiles_files"));
            log.debug("Updated index for file: {}", file.getId());
        } catch (Exception e) {
            log.warn("Failed to update index for file {}: {}", file.getId(), e.getMessage());
        }
    }

    private FileDocument toDocument(FileInfo file) {
        FileDocument doc = FileDocument.builder()
                .id(file.getId())
                .originalName(file.getOriginalName())
                .fileName(file.getFileName())
                .fileType(file.getFileType())
                .fileExtension(file.getFileExtension())
                .mimeType(file.getMimeType())
                .fileSize(file.getFileSize())
                .storageSpace(file.getStorageSpace())
                .folderId(file.getFolderId())
                .uploadUserId(file.getUploadUserId())
                .md5(file.getMd5())
                .downloadCount(file.getDownloadCount())
                .createdAt(file.getCreatedAt())
                .build();
        if (file.getUploadUserId() != null) {
            User user = userMapper.selectById(file.getUploadUserId());
            if (user != null) {
                doc.setUploadUsername(user.getNickname() != null ? user.getNickname() : user.getUsername());
            }
        }
        return doc;
    }
}
