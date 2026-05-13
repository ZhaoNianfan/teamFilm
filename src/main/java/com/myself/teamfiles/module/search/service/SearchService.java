package com.myself.teamfiles.module.search.service;

import com.myself.teamfiles.common.result.PageResult;
import com.myself.teamfiles.module.search.dto.SearchDTO;
import com.myself.teamfiles.module.search.dto.SearchResultVO;
import com.myself.teamfiles.module.search.dto.SearchTemplateDTO;
import com.myself.teamfiles.module.search.entity.SearchHistory;
import com.myself.teamfiles.module.search.entity.SearchTemplate;
import com.myself.teamfiles.module.search.document.FileDocument;

import java.util.List;

public interface SearchService {
    PageResult<SearchResultVO> search(SearchDTO dto);
    List<String> suggest(String keyword);
    List<SearchHistory> getHistory();
    void deleteHistory(Long id);
    void clearHistory();
    SearchTemplate saveTemplate(SearchTemplateDTO dto);
    List<SearchTemplate> getTemplates();
    SearchTemplate updateTemplate(Long id, SearchTemplateDTO dto);
    void deleteTemplate(Long id);
    PageResult<SearchResultVO> executeTemplate(Long id);
    List<SearchResultVO> similarFiles(Long fileId);
    void rebuildIndex();
    void indexFile(FileDocument doc);
    void deleteIndex(Long fileId);
}
