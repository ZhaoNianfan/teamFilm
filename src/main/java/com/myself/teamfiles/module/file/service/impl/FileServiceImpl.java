package com.myself.teamfiles.module.file.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.myself.teamfiles.common.constant.FileConstants;
import com.myself.teamfiles.common.enums.StorageSpaceEnum;
import com.myself.teamfiles.common.exception.BusinessException;
import com.myself.teamfiles.common.exception.ErrorCode;
import com.myself.teamfiles.common.result.PageResult;
import com.myself.teamfiles.common.util.FileUtil;
import com.myself.teamfiles.common.util.ZipUtil;
import com.myself.teamfiles.module.file.dto.*;
import com.myself.teamfiles.module.file.entity.FileChunk;
import com.myself.teamfiles.module.file.entity.FileInfo;
import com.myself.teamfiles.module.file.entity.Folder;
import com.myself.teamfiles.module.file.mapper.FileChunkMapper;
import com.myself.teamfiles.module.file.mapper.FileInfoMapper;
import com.myself.teamfiles.module.file.mapper.FolderMapper;
import com.myself.teamfiles.module.file.service.FileService;
import com.myself.teamfiles.module.recycle.entity.RecycleBin;
import com.myself.teamfiles.module.recycle.mapper.RecycleBinMapper;
import com.myself.teamfiles.module.search.service.FileIndexSyncService;
import com.myself.teamfiles.module.tag.dto.TagVO;
import com.myself.teamfiles.module.tag.entity.FileTag;
import com.myself.teamfiles.module.tag.entity.Tag;
import com.myself.teamfiles.module.tag.mapper.FileTagMapper;
import com.myself.teamfiles.module.tag.mapper.TagMapper;
import com.myself.teamfiles.module.user.entity.User;
import com.myself.teamfiles.module.user.mapper.UserMapper;
import com.myself.teamfiles.security.JwtContextHolder;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private final FileInfoMapper fileInfoMapper;
    private final FolderMapper folderMapper;
    private final FileChunkMapper fileChunkMapper;
    private final RecycleBinMapper recycleBinMapper;
    private final UserMapper userMapper;
    private final FileTagMapper fileTagMapper;
    private final TagMapper tagMapper;
    private final FileIndexSyncService fileIndexSyncService;

    @Value("${file.storage.base-path}")
    private String basePath;

    @Value("${file.storage.chunk-path}")
    private String chunkPath;

    @Value("${file.chunk-size:5242880}")
    private long chunkSize;

    @Override
    @Transactional
    public FileVO upload(MultipartFile file, String storageSpace, Long folderId) {
        Long userId = JwtContextHolder.getUserId();
        validateFileSize(file.getSize());
        validateStorageSpace(storageSpace, userId);

        String ext = FileUtil.getExtension(file.getOriginalFilename());
        String md5;
        try {
            md5 = FileUtil.md5(file.getInputStream());
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, "Failed to read file");
        }

        // MD5 dedup
        FileInfo existing = fileInfoMapper.selectOne(
                new LambdaQueryWrapper<FileInfo>().eq(FileInfo::getMd5, md5)
                        .eq(FileInfo::getDeleted, 0));
        if (existing != null) {
            return toVO(existing);
        }

        String relativePath = FileUtil.generateStoragePath(storageSpace, ext);
        Path targetPath = Paths.get(basePath, relativePath);
        try {
            FileUtil.copyToFile(file.getInputStream(), targetPath);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, "Failed to save file");
        }

        FileInfo fileInfo = new FileInfo();
        fileInfo.setFileName(targetPath.getFileName().toString());
        fileInfo.setOriginalName(file.getOriginalFilename());
        fileInfo.setFilePath(relativePath);
        fileInfo.setFileSize(file.getSize());
        fileInfo.setFileType(FileUtil.getFileType(ext));
        fileInfo.setMimeType(FileUtil.getMimeType(ext));
        fileInfo.setFileExtension(ext);
        fileInfo.setMd5(md5);
        fileInfo.setStorageSpace(storageSpace);
        fileInfo.setFolderId(folderId);
        fileInfo.setUploadUserId(userId);
        fileInfoMapper.insert(fileInfo);
        fileIndexSyncService.indexFile(fileInfo);

        log.info("File uploaded: id={}, name={}, size={}", fileInfo.getId(), file.getOriginalFilename(), file.getSize());
        return toVO(fileInfo);
    }

    @Override
    @Transactional
    public List<FileVO> batchUpload(List<MultipartFile> files, String storageSpace, Long folderId) {
        if (files.size() > FileConstants.MAX_BATCH_UPLOAD) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Max batch upload is " + FileConstants.MAX_BATCH_UPLOAD);
        }
        List<FileVO> results = new ArrayList<>();
        for (MultipartFile file : files) {
            results.add(upload(file, storageSpace, folderId));
        }
        return results;
    }

    @Override
    @Transactional
    public ChunkInitVO initChunkUpload(ChunkInitDTO dto) {
        Long userId = JwtContextHolder.getUserId();
        String storageSpace = dto.getStorageSpace() != null ? dto.getStorageSpace() : StorageSpaceEnum.PERSONAL.name();
        validateStorageSpace(storageSpace, userId);

        // Check MD5 for skip upload
        if (StrUtil.isNotBlank(dto.getFileMd5())) {
            FileInfo existing = fileInfoMapper.selectOne(
                    new LambdaQueryWrapper<FileInfo>().eq(FileInfo::getMd5, dto.getFileMd5())
                            .eq(FileInfo::getDeleted, 0));
            if (existing != null) {
                return ChunkInitVO.builder()
                        .uploadId(UUID.randomUUID().toString())
                        .chunkCount(0)
                        .chunkSize(chunkSize)
                        .skipUpload(true)
                        .build();
            }
        }

        String uploadId = UUID.randomUUID().toString();
        int chunkCount = (int) Math.ceil((double) dto.getFileSize() / chunkSize);

        // Save chunk records
        for (int i = 0; i < chunkCount; i++) {
            FileChunk chunk = new FileChunk();
            chunk.setUploadId(uploadId);
            chunk.setFileName(dto.getFileName());
            chunk.setFileMd5(dto.getFileMd5());
            chunk.setChunkIndex(i);
            chunk.setChunkCount(chunkCount);
            chunk.setChunkSize(i == chunkCount - 1 ? dto.getFileSize() - (long) i * chunkSize : chunkSize);
            chunk.setStatus(0);
            chunk.setUploadUserId(userId);
            chunk.setStorageSpace(storageSpace);
            chunk.setFolderId(dto.getFolderId());
            chunk.setExpiredAt(LocalDateTime.now().plusHours(24));
            fileChunkMapper.insert(chunk);
        }

        return ChunkInitVO.builder()
                .uploadId(uploadId)
                .chunkCount(chunkCount)
                .chunkSize(chunkSize)
                .skipUpload(false)
                .build();
    }

    @Override
    @Transactional
    public void uploadChunk(String uploadId, Integer chunkIndex, MultipartFile chunk) {
        FileChunk fileChunk = fileChunkMapper.selectOne(
                new LambdaQueryWrapper<FileChunk>()
                        .eq(FileChunk::getUploadId, uploadId)
                        .eq(FileChunk::getChunkIndex, chunkIndex));
        if (fileChunk == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Chunk record not found");
        }

        Path chunkDir = Paths.get(chunkPath, uploadId);
        Path chunkFile = chunkDir.resolve(String.valueOf(chunkIndex));
        try {
            FileUtil.copyToFile(chunk.getInputStream(), chunkFile);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, "Failed to save chunk");
        }

        fileChunk.setChunkPath(chunkFile.toString());
        fileChunk.setStatus(1);
        fileChunkMapper.updateById(fileChunk);
    }

    @Override
    @Transactional
    public FileVO mergeChunks(String uploadId) {
        List<FileChunk> chunks = fileChunkMapper.selectList(
                new LambdaQueryWrapper<FileChunk>().eq(FileChunk::getUploadId, uploadId)
                        .orderByAsc(FileChunk::getChunkIndex));

        if (chunks.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Upload task not found");
        }

        // Verify all chunks uploaded
        long notUploaded = chunks.stream().filter(c -> c.getStatus() == 0).count();
        if (notUploaded > 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "Not all chunks uploaded: " + notUploaded + " remaining");
        }

        FileChunk firstChunk = chunks.get(0);
        String origName = firstChunk.getFileName();
        String ext = FileUtil.getExtension(origName);
        String storageSpace = firstChunk.getStorageSpace() != null ?
                firstChunk.getStorageSpace() : StorageSpaceEnum.PERSONAL.name();
        String relativePath = FileUtil.generateStoragePath(storageSpace, ext);
        Path targetPath = Paths.get(basePath, relativePath);

        try {
            FileUtil.ensureDirectoryExists(targetPath.getParent());
            try (OutputStream os = Files.newOutputStream(targetPath)) {
                for (FileChunk chunk : chunks) {
                    Files.copy(Paths.get(chunk.getChunkPath()), os);
                }
            }
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, "Failed to merge chunks");
        }

        // Calculate final MD5
        String md5 = FileUtil.md5(targetPath);
        // Check MD5 dedup again
        FileInfo existing = fileInfoMapper.selectOne(
                new LambdaQueryWrapper<FileInfo>().eq(FileInfo::getMd5, md5)
                        .eq(FileInfo::getDeleted, 0));
        if (existing != null) {
            // Delete merged file, reuse existing
            try { Files.deleteIfExists(targetPath); } catch (IOException ignored) {}
            cleanupChunks(uploadId);
            return toVO(existing);
        }

        long totalSize = chunks.stream().mapToLong(FileChunk::getChunkSize).sum();

        FileInfo fileInfo = new FileInfo();
        fileInfo.setFileName(targetPath.getFileName().toString());
        fileInfo.setOriginalName(origName);
        fileInfo.setFilePath(relativePath);
        fileInfo.setFileSize(totalSize);
        fileInfo.setFileType(FileUtil.getFileType(ext));
        fileInfo.setMimeType(FileUtil.getMimeType(ext));
        fileInfo.setFileExtension(ext);
        fileInfo.setMd5(md5);
        fileInfo.setStorageSpace(storageSpace);
        fileInfo.setFolderId(firstChunk.getFolderId());
        fileInfo.setUploadUserId(firstChunk.getUploadUserId());
        fileInfoMapper.insert(fileInfo);
        fileIndexSyncService.indexFile(fileInfo);

        cleanupChunks(uploadId);
        log.info("Chunk upload completed: id={}, name={}", fileInfo.getId(), origName);
        return toVO(fileInfo);
    }

    private void cleanupChunks(String uploadId) {
        List<FileChunk> chunks = fileChunkMapper.selectList(
                new LambdaQueryWrapper<FileChunk>().eq(FileChunk::getUploadId, uploadId));
        for (FileChunk chunk : chunks) {
            try { Files.deleteIfExists(Paths.get(chunk.getChunkPath())); } catch (IOException ignored) {}
        }
        fileChunkMapper.delete(new LambdaQueryWrapper<FileChunk>().eq(FileChunk::getUploadId, uploadId));
        try { Files.deleteIfExists(Paths.get(chunkPath, uploadId)); } catch (IOException ignored) {}
    }

    @Override
    @Transactional
    public void cancelChunkUpload(String uploadId) {
        cleanupChunks(uploadId);
    }

    @Override
    public Object getChunkProgress(String uploadId) {
        List<FileChunk> chunks = fileChunkMapper.selectList(
                new LambdaQueryWrapper<FileChunk>().eq(FileChunk::getUploadId, uploadId));
        if (chunks.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Upload task not found");
        }
        long uploaded = chunks.stream().filter(c -> c.getStatus() == 1).count();
        Map<String, Object> result = new HashMap<>();
        result.put("uploadId", uploadId);
        result.put("total", chunks.size());
        result.put("uploaded", uploaded);
        result.put("completed", uploaded == chunks.size());
        return result;
    }

    @Override
    public PageResult<FileVO> listFiles(FilePageDTO dto) {
        Long userId = JwtContextHolder.getUserId();
        LambdaQueryWrapper<FileInfo> wrapper = new LambdaQueryWrapper<FileInfo>()
                .eq(FileInfo::getDeleted, 0);

        // Filter by space
        String role = JwtContextHolder.getRole();
        if (StrUtil.isNotBlank(dto.getStorageSpace())) {
            if (StorageSpaceEnum.PERSONAL.name().equals(dto.getStorageSpace())) {
                // Personal: files uploaded by this user (regardless of storageSpace)
                wrapper.eq(FileInfo::getUploadUserId, userId);
            } else if (StorageSpaceEnum.TEAM.name().equals(dto.getStorageSpace())) {
                if ("ADMIN".equals(role)) {
                    // Admin team view: all files in system
                } else {
                    wrapper.eq(FileInfo::getStorageSpace, StorageSpaceEnum.TEAM.name());
                }
            }
        } else {
            // Default: personal files or team files
            wrapper.and(w -> w.eq(FileInfo::getUploadUserId, userId)
                    .or().eq(FileInfo::getStorageSpace, StorageSpaceEnum.TEAM.name()));
        }

        if (dto.getFolderId() != null) {
            wrapper.eq(FileInfo::getFolderId, dto.getFolderId());
        }
        if (StrUtil.isNotBlank(dto.getKeyword())) {
            wrapper.like(FileInfo::getOriginalName, dto.getKeyword());
        }
        if (StrUtil.isNotBlank(dto.getFileType())) {
            wrapper.eq(FileInfo::getFileType, dto.getFileType());
        }

        // Sort
        if ("asc".equalsIgnoreCase(dto.getOrderDir())) {
            wrapper.orderByAsc(FileInfo::getCreatedAt);
        } else {
            wrapper.orderByDesc(FileInfo::getCreatedAt);
        }

        Page<FileInfo> page = new Page<>(dto.getPage(), dto.getSize());
        IPage<FileInfo> result = fileInfoMapper.selectPage(page, wrapper);

        List<FileVO> records = result.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        // Batch load tags
        List<Long> fileIds = records.stream().map(FileVO::getId).toList();
        if (!fileIds.isEmpty()) {
            List<FileTag> allTags = fileTagMapper.selectList(
                    new LambdaQueryWrapper<FileTag>().in(FileTag::getFileId, fileIds));
            Map<Long, List<FileTag>> tagMap = allTags.stream().collect(Collectors.groupingBy(FileTag::getFileId));
            Set<Long> tagIds = allTags.stream().map(FileTag::getTagId).collect(Collectors.toSet());
            Map<Long, Tag> tagEntities = new HashMap<>();
            if (!tagIds.isEmpty()) {
                tagMapper.selectBatchIds(tagIds).forEach(t -> tagEntities.put(t.getId(), t));
            }
            for (FileVO vo : records) {
                List<FileTag> ftags = tagMap.getOrDefault(vo.getId(), Collections.emptyList());
                List<TagVO> tagVOs = new ArrayList<>();
                for (FileTag ft : ftags) {
                    Tag t = tagEntities.get(ft.getTagId());
                    if (t != null && t.getDeleted() == 0) {
                        TagVO tv = new TagVO();
                        tv.setId(t.getId()); tv.setTagName(t.getTagName()); tv.setColor(t.getColor());
                        tagVOs.add(tv);
                    }
                }
                vo.setTags(tagVOs);
            }
        }
        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), records);
    }

    @Override
    public FileVO getFile(Long id) {
        FileInfo fileInfo = fileInfoMapper.selectById(id);
        if (fileInfo == null || fileInfo.getDeleted() == 1) {
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
        }
        checkFileAccess(fileInfo);
        return toVO(fileInfo);
    }

    @Override
    @Transactional
    public void rename(Long id, String newName) {
        FileInfo fileInfo = getFileEntity(id);
        checkFileWriteAccess(fileInfo);
        fileInfo.setOriginalName(newName);
        String ext = FileUtil.getExtension(newName);
        if (StrUtil.isNotBlank(ext)) {
            fileInfo.setFileExtension(ext);
            fileInfo.setFileType(FileUtil.getFileType(ext));
            fileInfo.setMimeType(FileUtil.getMimeType(ext));
        }
        fileInfoMapper.updateById(fileInfo);
        fileIndexSyncService.updateIndex(fileInfo);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        FileInfo fileInfo = getFileEntity(id);
        checkFileWriteAccess(fileInfo);

        // Move to recycle bin
        RecycleBin recycleBin = new RecycleBin();
        recycleBin.setOriginalType("FILE");
        recycleBin.setOriginalId(fileInfo.getId());
        recycleBin.setFileName(fileInfo.getOriginalName());
        recycleBin.setFilePath(fileInfo.getFilePath());
        recycleBin.setFileSize(fileInfo.getFileSize());
        recycleBin.setStorageSpace(fileInfo.getStorageSpace());
        recycleBin.setOriginalParentId(fileInfo.getFolderId());
        recycleBin.setDeletedBy(JwtContextHolder.getUserId());
        recycleBin.setDeletedAt(LocalDateTime.now());
        recycleBin.setExpireAt(LocalDateTime.now().plusDays(FileConstants.RECYCLE_RETENTION_DAYS));
        recycleBinMapper.insert(recycleBin);

        // Logical delete file
        fileInfoMapper.deleteById(id);
        fileIndexSyncService.deleteIndex(id);
    }

    @Override
    @Transactional
    public FileVO copy(Long id, Long targetFolderId) {
        FileInfo fileInfo = getFileEntity(id);
        checkFileAccess(fileInfo);

        if (targetFolderId != null && targetFolderId > 0) {
            Folder folder = folderMapper.selectById(targetFolderId);
            if (folder == null || folder.getDeleted() == 1) {
                throw new BusinessException(ErrorCode.FOLDER_NOT_FOUND);
            }
        }

        String ext = fileInfo.getFileExtension();
        String newPath = FileUtil.generateStoragePath(fileInfo.getStorageSpace(), ext);
        Path sourcePath = Paths.get(basePath, fileInfo.getFilePath());
        Path targetPath = Paths.get(basePath, newPath);
        try {
            FileUtil.ensureDirectoryExists(targetPath.getParent());
            Files.copy(sourcePath, targetPath);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, "Failed to copy file");
        }

        FileInfo copy = new FileInfo();
        copy.setFileName(targetPath.getFileName().toString());
        copy.setOriginalName(fileInfo.getOriginalName());
        copy.setFilePath(newPath);
        copy.setFileSize(fileInfo.getFileSize());
        copy.setFileType(fileInfo.getFileType());
        copy.setMimeType(fileInfo.getMimeType());
        copy.setFileExtension(ext);
        copy.setMd5(fileInfo.getMd5());
        copy.setStorageSpace(fileInfo.getStorageSpace());
        copy.setFolderId(targetFolderId);
        copy.setUploadUserId(JwtContextHolder.getUserId());
        fileInfoMapper.insert(copy);

        return toVO(copy);
    }

    @Override
    @Transactional
    public FileVO move(Long id, Long targetFolderId) {
        FileInfo fileInfo = getFileEntity(id);
        checkFileWriteAccess(fileInfo);

        if (targetFolderId != null && targetFolderId > 0) {
            Folder folder = folderMapper.selectById(targetFolderId);
            if (folder == null || folder.getDeleted() == 1) {
                throw new BusinessException(ErrorCode.FOLDER_NOT_FOUND);
            }
        }
        fileInfo.setFolderId(targetFolderId);
        fileInfoMapper.updateById(fileInfo);
        try { fileIndexSyncService.updateIndex(fileInfo); } catch (Exception ignored) {}
        return toVO(fileInfo);
    }

    @Override
    @Transactional
    public FileVO moveToTeam(Long id) {
        FileInfo fileInfo = getFileEntity(id);
        checkFileWriteAccess(fileInfo);
        if (StorageSpaceEnum.TEAM.name().equals(fileInfo.getStorageSpace())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "File is already in team space");
        }

        // Move physical file
        Path sourcePath = Paths.get(basePath, fileInfo.getFilePath());
        String ext = fileInfo.getFileExtension();
        String newPath = FileUtil.generateStoragePath(StorageSpaceEnum.TEAM.name(), ext);
        Path targetPath = Paths.get(basePath, newPath);
        try {
            FileUtil.ensureDirectoryExists(targetPath.getParent());
            Files.move(sourcePath, targetPath);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, "Failed to move file to team space");
        }

        fileInfo.setFilePath(newPath);
        fileInfo.setFileName(targetPath.getFileName().toString());
        fileInfo.setStorageSpace(StorageSpaceEnum.TEAM.name());
        fileInfoMapper.updateById(fileInfo);
        fileIndexSyncService.updateIndex(fileInfo);
        return toVO(fileInfo);
    }

    @Override
    public void preview(Long id, HttpServletResponse response) {
        FileInfo fileInfo = getFileEntity(id);
        checkFileAccess(fileInfo);
        Path filePath = Paths.get(basePath, fileInfo.getFilePath());
        if (!Files.exists(filePath)) {
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
        }

        String mimeType = fileInfo.getMimeType();
        if (mimeType == null) mimeType = "application/octet-stream";

        try {
            response.setContentType(mimeType);
            response.setHeader("Cache-Control", "max-age=86400");
            response.setHeader("Content-Disposition", "inline; filename=\"" +
                    URLEncoder.encode(fileInfo.getOriginalName(), StandardCharsets.UTF_8) + "\"");
            Files.copy(filePath, response.getOutputStream());
            response.getOutputStream().flush();

            fileInfo.setPreviewCount((fileInfo.getPreviewCount() == null ? 0 : fileInfo.getPreviewCount()) + 1);
            fileInfoMapper.updateById(fileInfo);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, "Preview failed");
        }
    }

    @Override
    public void download(Long id, HttpServletResponse response) {
        FileInfo fileInfo = getFileEntity(id);
        checkFileAccess(fileInfo);
        Path filePath = Paths.get(basePath, fileInfo.getFilePath());
        if (!Files.exists(filePath)) {
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
        }

        try {
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition",
                    "attachment; filename=\"" + URLEncoder.encode(fileInfo.getOriginalName(), StandardCharsets.UTF_8) + "\"");
            response.setContentLengthLong(fileInfo.getFileSize());
            Files.copy(filePath, response.getOutputStream());
            response.getOutputStream().flush();

            fileInfo.setDownloadCount((fileInfo.getDownloadCount() == null ? 0 : fileInfo.getDownloadCount()) + 1);
            fileInfoMapper.updateById(fileInfo);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, "Download failed");
        }
    }

    @Override
    @Async
    public void batchDownload(List<Long> ids, HttpServletResponse response) {
        List<FileInfo> files = fileInfoMapper.selectBatchIds(ids);
        if (files.isEmpty()) {
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
        }

        String zipName = "files_" + System.currentTimeMillis() + ".zip";
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"" + URLEncoder.encode(zipName, StandardCharsets.UTF_8) + "\"");

        List<ZipUtil.PathEntry> entries = new ArrayList<>();
        Set<String> usedNames = new HashSet<>();
        for (FileInfo file : files) {
            Path filePath = Paths.get(basePath, file.getFilePath());
            if (!Files.exists(filePath)) continue;
            String entryName = file.getOriginalName();
            if (usedNames.contains(entryName)) {
                String ext = FileUtil.getExtension(entryName);
                String base = entryName.substring(0, entryName.length() - ext.length() - 1);
                entryName = base + "_" + file.getId() + (ext.isEmpty() ? "" : "." + ext);
            }
            usedNames.add(entryName);
            entries.add(new ZipUtil.PathEntry(filePath, entryName));
        }

        try {
            ZipUtil.createZip(entries, response.getOutputStream());
            response.getOutputStream().flush();
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, "Batch download failed");
        }
    }

    @Override
    public List<FileVO> getSimilarFiles(Long id) {
        FileInfo fileInfo = getFileEntity(id);
        checkFileAccess(fileInfo);
        // Find files with same MD5 or similar name
        List<FileInfo> similar = fileInfoMapper.selectList(
                new LambdaQueryWrapper<FileInfo>()
                        .eq(FileInfo::getDeleted, 0)
                        .and(w -> w.eq(FileInfo::getMd5, fileInfo.getMd5())
                                .or().eq(FileInfo::getFileExtension, fileInfo.getFileExtension()))
                        .ne(FileInfo::getId, id)
                        .last("LIMIT 20"));
        return similar.stream().map(this::toVO).collect(Collectors.toList());
    }

    // --- helper methods ---

    private FileInfo getFileEntity(Long id) {
        FileInfo fileInfo = fileInfoMapper.selectById(id);
        if (fileInfo == null || fileInfo.getDeleted() == 1) {
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
        }
        return fileInfo;
    }

    private void checkFileAccess(FileInfo fileInfo) {
        Long userId = JwtContextHolder.getUserId();
        String role = JwtContextHolder.getRole();
        if ("ADMIN".equals(role)) return;
        if (StorageSpaceEnum.PERSONAL.name().equals(fileInfo.getStorageSpace())
                && !fileInfo.getUploadUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Cannot access other user's personal files");
        }
    }

    private void checkFileWriteAccess(FileInfo fileInfo) {
        Long userId = JwtContextHolder.getUserId();
        String role = JwtContextHolder.getRole();
        if ("ADMIN".equals(role)) return;
        if (StorageSpaceEnum.PERSONAL.name().equals(fileInfo.getStorageSpace())) {
            if (!fileInfo.getUploadUserId().equals(userId)) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "Cannot modify other user's personal files");
            }
        } else if (StorageSpaceEnum.TEAM.name().equals(fileInfo.getStorageSpace())) {
            if (!"FORMAL".equals(role)) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "Guest users cannot modify team files");
            }
        }
    }

    private void validateFileSize(long size) {
        if (size > FileConstants.MAX_FILE_SIZE) {
            throw new BusinessException(ErrorCode.FILE_SIZE_EXCEEDED,
                    "File size exceeds limit: " + FileUtil.formatFileSize(FileConstants.MAX_FILE_SIZE));
        }
    }

    private void validateStorageSpace(String storageSpace, Long userId) {
        if (StorageSpaceEnum.TEAM.name().equals(storageSpace)) {
            String role = JwtContextHolder.getRole();
            if ("GUEST".equals(role)) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "Guest users cannot upload to team space");
            }
        }
    }

    private boolean isImage(FileInfo fileInfo) {
        return "IMAGE".equals(fileInfo.getFileType());
    }

    private FileVO toVO(FileInfo fileInfo) {
        FileVO vo = new FileVO();
        vo.setId(fileInfo.getId());
        vo.setFileName(fileInfo.getFileName());
        vo.setOriginalName(fileInfo.getOriginalName());
        vo.setFileSize(fileInfo.getFileSize());
        vo.setFileType(fileInfo.getFileType());
        vo.setMimeType(fileInfo.getMimeType());
        vo.setFileExtension(fileInfo.getFileExtension());
        vo.setMd5(fileInfo.getMd5());
        vo.setStorageSpace(fileInfo.getStorageSpace());
        vo.setFolderId(fileInfo.getFolderId());
        vo.setUploadUserId(fileInfo.getUploadUserId());
        vo.setDownloadCount(fileInfo.getDownloadCount());
        vo.setPreviewCount(fileInfo.getPreviewCount());
        vo.setCreatedAt(fileInfo.getCreatedAt());
        vo.setUpdatedAt(fileInfo.getUpdatedAt());

        // Resolve uploader username
        if (fileInfo.getUploadUserId() != null) {
            User user = userMapper.selectById(fileInfo.getUploadUserId());
            if (user != null) {
                vo.setUploadUsername(user.getNickname() != null ? user.getNickname() : user.getUsername());
            }
        }
        // Mark as shared if in team space but uploaded by this user
        vo.setShared(StorageSpaceEnum.TEAM.name().equals(fileInfo.getStorageSpace()));
        // Resolve folder name
        if (fileInfo.getFolderId() != null && fileInfo.getFolderId() > 0) {
            Folder folder = folderMapper.selectById(fileInfo.getFolderId());
            if (folder != null) {
                vo.setFolderName(folder.getFolderName());
            }
        }
        return vo;
    }
}
