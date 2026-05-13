package com.myself.teamfiles.module.file.controller;

import com.myself.teamfiles.common.annotation.OperationLog;
import com.myself.teamfiles.common.result.PageResult;
import com.myself.teamfiles.common.result.R;
import com.myself.teamfiles.module.file.dto.*;
import com.myself.teamfiles.module.file.service.FileService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @OperationLog(module = "FILE", operation = "Upload file")
    @PostMapping("/upload")
    public R<FileVO> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "storageSpace", defaultValue = "PERSONAL") String storageSpace,
            @RequestParam(value = "folderId", required = false) Long folderId) {
        return R.ok(fileService.upload(file, storageSpace, folderId));
    }

    @OperationLog(module = "FILE", operation = "Batch upload files")
    @PostMapping("/batch-upload")
    public R<List<FileVO>> batchUpload(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(value = "storageSpace", defaultValue = "PERSONAL") String storageSpace,
            @RequestParam(value = "folderId", required = false) Long folderId) {
        return R.ok(fileService.batchUpload(files, storageSpace, folderId));
    }

    @OperationLog(module = "FILE", operation = "Initialize chunk upload")
    @PostMapping("/chunk/init")
    public R<ChunkInitVO> initChunkUpload(@Valid @RequestBody ChunkInitDTO dto) {
        return R.ok(fileService.initChunkUpload(dto));
    }

    @OperationLog(module = "FILE", operation = "Upload chunk")
    @PostMapping("/chunk/upload")
    public R<Void> uploadChunk(
            @RequestParam("uploadId") String uploadId,
            @RequestParam("chunkIndex") Integer chunkIndex,
            @RequestParam("chunk") MultipartFile chunk) {
        fileService.uploadChunk(uploadId, chunkIndex, chunk);
        return R.ok();
    }

    @OperationLog(module = "FILE", operation = "Merge chunks")
    @PostMapping("/chunk/merge")
    public R<FileVO> mergeChunks(@RequestParam("uploadId") String uploadId) {
        return R.ok(fileService.mergeChunks(uploadId));
    }

    @OperationLog(module = "FILE", operation = "Cancel chunk upload")
    @PostMapping("/chunk/cancel")
    public R<Void> cancelChunkUpload(@RequestParam("uploadId") String uploadId) {
        fileService.cancelChunkUpload(uploadId);
        return R.ok();
    }

    @GetMapping("/chunk/progress")
    public R<Object> getChunkProgress(@RequestParam("uploadId") String uploadId) {
        return R.ok(fileService.getChunkProgress(uploadId));
    }

    @GetMapping
    public R<PageResult<FileVO>> listFiles(FilePageDTO dto) {
        return R.ok(fileService.listFiles(dto));
    }

    @GetMapping("/{id}")
    public R<FileVO> getFile(@PathVariable Long id) {
        return R.ok(fileService.getFile(id));
    }

    @OperationLog(module = "FILE", operation = "Rename file")
    @PutMapping("/{id}/rename")
    public R<Void> rename(@PathVariable Long id, @RequestParam("name") String newName) {
        fileService.rename(id, newName);
        return R.ok();
    }

    @OperationLog(module = "FILE", operation = "Delete file")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        fileService.delete(id);
        return R.ok();
    }

    @OperationLog(module = "FILE", operation = "Copy file")
    @PostMapping("/{id}/copy")
    public R<FileVO> copy(@PathVariable Long id, @Valid @RequestBody MoveFileDTO dto) {
        return R.ok(fileService.copy(id, dto.getTargetFolderId()));
    }

    @OperationLog(module = "FILE", operation = "Move file")
    @PostMapping("/{id}/move")
    public R<FileVO> move(@PathVariable Long id, @Valid @RequestBody MoveFileDTO dto) {
        return R.ok(fileService.move(id, dto.getTargetFolderId()));
    }

    @OperationLog(module = "FILE", operation = "Move file to team space")
    @PostMapping("/{id}/move-to-team")
    public R<FileVO> moveToTeam(@PathVariable Long id) {
        return R.ok(fileService.moveToTeam(id));
    }

    @GetMapping("/{id}/preview")
    public void preview(@PathVariable Long id, HttpServletResponse response) {
        fileService.preview(id, response);
    }

    @GetMapping("/{id}/download")
    public void download(@PathVariable Long id, HttpServletResponse response) {
        fileService.download(id, response);
    }

    @OperationLog(module = "FILE", operation = "Batch download")
    @PostMapping("/batch-download")
    public void batchDownload(@RequestBody List<Long> ids, HttpServletResponse response) {
        fileService.batchDownload(ids, response);
    }

    @GetMapping("/{id}/similar")
    public R<List<FileVO>> getSimilarFiles(@PathVariable Long id) {
        return R.ok(fileService.getSimilarFiles(id));
    }
}
