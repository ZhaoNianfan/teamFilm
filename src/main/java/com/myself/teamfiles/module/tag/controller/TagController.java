package com.myself.teamfiles.module.tag.controller;

import com.myself.teamfiles.common.annotation.OperationLog;
import com.myself.teamfiles.common.result.R;
import com.myself.teamfiles.module.file.dto.FileVO;
import com.myself.teamfiles.module.tag.dto.*;
import com.myself.teamfiles.module.tag.service.TagService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    @OperationLog(module = "TAG", operation = "Create tag")
    @PostMapping
    public R<TagVO> create(@Valid @RequestBody TagDTO dto) {
        return R.ok(tagService.create(dto));
    }

    @GetMapping
    public R<List<TagVO>> listVisible() {
        return R.ok(tagService.listVisible());
    }

    @GetMapping("/{id}")
    public R<TagVO> getById(@PathVariable Long id) {
        return R.ok(tagService.getById(id));
    }

    @OperationLog(module = "TAG", operation = "Update tag")
    @PutMapping("/{id}")
    public R<TagVO> update(@PathVariable Long id, @Valid @RequestBody TagDTO dto) {
        return R.ok(tagService.update(id, dto));
    }

    @OperationLog(module = "TAG", operation = "Delete tag")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        tagService.delete(id);
        return R.ok();
    }

    @GetMapping("/{id}/files")
    public R<List<FileVO>> getFilesByTag(@PathVariable Long id) {
        return R.ok(tagService.getFilesByTag(id));
    }

    @GetMapping("/similar")
    public R<List<TagVO>> similar(@RequestParam("keyword") String keyword) {
        return R.ok(tagService.similar(keyword));
    }

    @GetMapping("/autocomplete")
    public R<List<TagVO>> autocomplete(@RequestParam("keyword") String keyword) {
        return R.ok(tagService.autocomplete(keyword));
    }

    @OperationLog(module = "TAG", operation = "Add tags to file")
    @PostMapping("/files/{fileId}/tags")
    public R<Void> addTagsToFile(@PathVariable Long fileId, @RequestBody List<Long> tagIds) {
        tagService.addTagsToFile(fileId, tagIds);
        return R.ok();
    }

    @OperationLog(module = "TAG", operation = "Remove tag from file")
    @DeleteMapping("/files/{fileId}/tags/{tagId}")
    public R<Void> removeTagFromFile(@PathVariable Long fileId, @PathVariable Long tagId) {
        tagService.removeTagFromFile(fileId, tagId);
        return R.ok();
    }

    @OperationLog(module = "TAG", operation = "Batch tag files")
    @PostMapping("/files/batch-tags")
    public R<Void> batchTags(@Valid @RequestBody BatchTagsDTO dto) {
        tagService.batchTags(dto);
        return R.ok();
    }

    // ==================== Tag Groups ====================

    @OperationLog(module = "TAG", operation = "Create tag group")
    @PostMapping("/groups")
    public R<TagGroupDTO> createGroup(@Valid @RequestBody TagGroupDTO dto) {
        return R.ok(tagService.createGroup(dto));
    }

    @GetMapping("/groups")
    public R<List<TagGroupDTO>> listGroups() {
        return R.ok(tagService.listGroups());
    }

    @OperationLog(module = "TAG", operation = "Update tag group")
    @PutMapping("/groups/{id}")
    public R<TagGroupDTO> updateGroup(@PathVariable Long id, @Valid @RequestBody TagGroupDTO dto) {
        return R.ok(tagService.updateGroup(id, dto));
    }

    @OperationLog(module = "TAG", operation = "Delete tag group")
    @DeleteMapping("/groups/{id}")
    public R<Void> deleteGroup(@PathVariable Long id) {
        tagService.deleteGroup(id);
        return R.ok();
    }

    @OperationLog(module = "TAG", operation = "Add tags to group")
    @PostMapping("/groups/{id}/tags")
    public R<Void> addTagsToGroup(@PathVariable Long id, @RequestBody List<Long> tagIds) {
        tagService.addTagsToGroup(id, tagIds);
        return R.ok();
    }

    @OperationLog(module = "TAG", operation = "Remove tag from group")
    @DeleteMapping("/groups/{id}/tags/{tagId}")
    public R<Void> removeTagFromGroup(@PathVariable Long id, @PathVariable Long tagId) {
        tagService.removeTagFromGroup(id, tagId);
        return R.ok();
    }
}
