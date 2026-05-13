package com.myself.teamfiles.module.search.controller;

import com.myself.teamfiles.common.annotation.OperationLog;
import com.myself.teamfiles.common.result.PageResult;
import com.myself.teamfiles.common.result.R;
import com.myself.teamfiles.module.search.dto.SearchDTO;
import com.myself.teamfiles.module.search.dto.SearchResultVO;
import com.myself.teamfiles.module.search.dto.SearchTemplateDTO;
import com.myself.teamfiles.module.search.entity.SearchHistory;
import com.myself.teamfiles.module.search.entity.SearchTemplate;
import com.myself.teamfiles.module.search.service.SearchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @OperationLog(module = "SEARCH", operation = "Search files")
    @PostMapping
    public R<PageResult<SearchResultVO>> search(@RequestBody SearchDTO dto) {
        return R.ok(searchService.search(dto));
    }

    @GetMapping("/suggest")
    public R<List<String>> suggest(@RequestParam("keyword") String keyword) {
        return R.ok(searchService.suggest(keyword));
    }

    @GetMapping("/history")
    public R<List<SearchHistory>> getHistory() {
        return R.ok(searchService.getHistory());
    }

    @OperationLog(module = "SEARCH", operation = "Delete search history")
    @DeleteMapping("/history/{id}")
    public R<Void> deleteHistory(@PathVariable Long id) {
        searchService.deleteHistory(id);
        return R.ok();
    }

    @OperationLog(module = "SEARCH", operation = "Clear search history")
    @DeleteMapping("/history")
    public R<Void> clearHistory() {
        searchService.clearHistory();
        return R.ok();
    }

    @OperationLog(module = "SEARCH", operation = "Save search template")
    @PostMapping("/templates")
    public R<SearchTemplate> saveTemplate(@Valid @RequestBody SearchTemplateDTO dto) {
        return R.ok(searchService.saveTemplate(dto));
    }

    @GetMapping("/templates")
    public R<List<SearchTemplate>> getTemplates() {
        return R.ok(searchService.getTemplates());
    }

    @OperationLog(module = "SEARCH", operation = "Update search template")
    @PutMapping("/templates/{id}")
    public R<SearchTemplate> updateTemplate(@PathVariable Long id, @Valid @RequestBody SearchTemplateDTO dto) {
        return R.ok(searchService.updateTemplate(id, dto));
    }

    @OperationLog(module = "SEARCH", operation = "Delete search template")
    @DeleteMapping("/templates/{id}")
    public R<Void> deleteTemplate(@PathVariable Long id) {
        searchService.deleteTemplate(id);
        return R.ok();
    }

    @OperationLog(module = "SEARCH", operation = "Execute search template")
    @PostMapping("/templates/{id}/execute")
    public R<PageResult<SearchResultVO>> executeTemplate(@PathVariable Long id) {
        return R.ok(searchService.executeTemplate(id));
    }

    @GetMapping("/similar/{fileId}")
    public R<List<SearchResultVO>> similarFiles(@PathVariable Long fileId) {
        return R.ok(searchService.similarFiles(fileId));
    }

    @OperationLog(module = "SEARCH", operation = "Rebuild search index")
    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping("/admin/rebuild-index")
    public R<String> rebuildIndex() {
        searchService.rebuildIndex();
        return R.ok("Index rebuild started");
    }
}
