package com.myself.teamfiles.module.file.controller;

import com.myself.teamfiles.common.annotation.OperationLog;
import com.myself.teamfiles.common.result.R;
import com.myself.teamfiles.module.file.dto.FolderDTO;
import com.myself.teamfiles.module.file.service.FolderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/folders")
@RequiredArgsConstructor
public class FolderController {

    private final FolderService folderService;

    @OperationLog(module = "FILE", operation = "Create folder")
    @PostMapping
    public R<FolderDTO> create(@Valid @RequestBody FolderDTO dto) {
        return R.ok(folderService.create(dto));
    }

    @GetMapping
    public R<List<FolderDTO>> list(
            @RequestParam(value = "storageSpace", required = false) String storageSpace,
            @RequestParam(value = "parentId", required = false) Long parentId) {
        return R.ok(folderService.list(storageSpace, parentId));
    }

    @GetMapping("/{id}")
    public R<FolderDTO> getById(@PathVariable Long id) {
        return R.ok(folderService.getById(id));
    }

    @OperationLog(module = "FILE", operation = "Rename folder")
    @PutMapping("/{id}/rename")
    public R<Void> rename(@PathVariable Long id, @RequestParam("name") String newName) {
        folderService.rename(id, newName);
        return R.ok();
    }

    @OperationLog(module = "FILE", operation = "Delete folder")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        folderService.delete(id);
        return R.ok();
    }

    @GetMapping("/tree")
    public R<List<FolderDTO>> tree(
            @RequestParam(value = "storageSpace", required = false) String storageSpace) {
        return R.ok(folderService.tree(storageSpace));
    }

    @OperationLog(module = "FILE", operation = "Batch delete folders")
    @PostMapping("/batch-delete")
    public R<Void> batchDelete(@RequestBody List<Long> ids) {
        folderService.batchDelete(ids);
        return R.ok();
    }
}
