package com.myself.teamfiles.module.file.service;

import com.myself.teamfiles.common.result.PageResult;
import com.myself.teamfiles.module.file.dto.*;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FileService {
    FileVO upload(MultipartFile file, String storageSpace, Long folderId);
    List<FileVO> batchUpload(List<MultipartFile> files, String storageSpace, Long folderId);
    ChunkInitVO initChunkUpload(ChunkInitDTO dto);
    void uploadChunk(String uploadId, Integer chunkIndex, MultipartFile chunk);
    FileVO mergeChunks(String uploadId);
    void cancelChunkUpload(String uploadId);
    Object getChunkProgress(String uploadId);
    PageResult<FileVO> listFiles(FilePageDTO dto);
    FileVO getFile(Long id);
    void rename(Long id, String newName);
    void delete(Long id);
    FileVO copy(Long id, Long targetFolderId);
    FileVO move(Long id, Long targetFolderId);
    FileVO moveToTeam(Long id);
    void preview(Long id, HttpServletResponse response);
    void download(Long id, HttpServletResponse response);
    void batchDownload(List<Long> ids, HttpServletResponse response);
    List<FileVO> getSimilarFiles(Long id);
}
