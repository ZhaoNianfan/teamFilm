package com.myself.teamfiles.module.search.service.impl;

import cn.hutool.core.util.StrUtil;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myself.teamfiles.common.exception.BusinessException;
import com.myself.teamfiles.common.exception.ErrorCode;
import com.myself.teamfiles.common.result.PageResult;
import com.myself.teamfiles.module.file.entity.FileInfo;
import com.myself.teamfiles.module.file.mapper.FileInfoMapper;
import com.myself.teamfiles.module.search.document.FileDocument;
import com.myself.teamfiles.module.search.dto.SearchDTO;
import com.myself.teamfiles.module.search.dto.SearchResultVO;
import com.myself.teamfiles.module.search.dto.SearchTemplateDTO;
import com.myself.teamfiles.module.search.entity.SearchHistory;
import com.myself.teamfiles.module.search.entity.SearchTemplate;
import com.myself.teamfiles.module.search.mapper.SearchHistoryMapper;
import com.myself.teamfiles.module.search.mapper.SearchTemplateMapper;
import com.myself.teamfiles.module.search.service.SearchService;
import com.myself.teamfiles.module.tag.entity.FileTag;
import com.myself.teamfiles.module.tag.mapper.FileTagMapper;
import com.myself.teamfiles.module.user.entity.User;
import com.myself.teamfiles.module.user.mapper.UserMapper;
import com.myself.teamfiles.security.JwtContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.HighlightQuery;
import org.springframework.data.elasticsearch.core.query.highlight.Highlight;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightField;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SearchServiceImpl implements SearchService {

    private final ElasticsearchOperations elasticsearchOperations;
    private final FileInfoMapper fileInfoMapper;
    private final UserMapper userMapper;
    private final SearchHistoryMapper searchHistoryMapper;
    private final SearchTemplateMapper searchTemplateMapper;
    private final FileTagMapper fileTagMapper;
    private final boolean esAvailable;

    public SearchServiceImpl(@org.springframework.beans.factory.annotation.Autowired(required = false) ElasticsearchOperations elasticsearchOperations,
                             FileInfoMapper fileInfoMapper, UserMapper userMapper,
                             SearchHistoryMapper searchHistoryMapper, SearchTemplateMapper searchTemplateMapper,
                             FileTagMapper fileTagMapper) {
        this.elasticsearchOperations = elasticsearchOperations;
        this.fileInfoMapper = fileInfoMapper;
        this.userMapper = userMapper;
        this.searchHistoryMapper = searchHistoryMapper;
        this.searchTemplateMapper = searchTemplateMapper;
        this.fileTagMapper = fileTagMapper;
        this.esAvailable = elasticsearchOperations != null;
    }

    @Override
    public PageResult<SearchResultVO> search(SearchDTO dto) {
        Long userId = JwtContextHolder.getUserId();
        if (StrUtil.isBlank(dto.getKeyword()) && (dto.getTagIds() == null || dto.getTagIds().isEmpty())) {
            return PageResult.of(0, 1, dto.getSize(), Collections.emptyList());
        }

        // MySQL fallback when ES unavailable
        if (!esAvailable) {
            return mysqlSearch(dto, userId);
        }

        NativeQueryBuilder queryBuilder = NativeQuery.builder();
        BoolQuery.Builder boolBuilder = new BoolQuery.Builder();

        // Keyword search
        if (StrUtil.isNotBlank(dto.getKeyword())) {
            boolBuilder.must(Query.of(q -> q.multiMatch(mm -> mm
                    .fields("originalName^3", "fileName")
                    .query(dto.getKeyword())
                    .type(TextQueryType.BestFields)
                    .operator(Operator.And))));
        }

        // File type filter
        if (StrUtil.isNotBlank(dto.getFileType())) {
            boolBuilder.filter(Query.of(q -> q.term(t -> t.field("fileType").value(dto.getFileType()))));
        }
        if (StrUtil.isNotBlank(dto.getFileExtension())) {
            boolBuilder.filter(Query.of(q -> q.term(t -> t.field("fileExtension").value(dto.getFileExtension()))));
        }
        if (StrUtil.isNotBlank(dto.getStorageSpace())) {
            boolBuilder.filter(Query.of(q -> q.term(t -> t.field("storageSpace").value(dto.getStorageSpace()))));
        }

        // Size range (TODO: fix ES client API for range queries)
        if (dto.getMinSize() != null || dto.getMaxSize() != null) {
            log.debug("Size filter requested: min={}, max={} — API pending", dto.getMinSize(), dto.getMaxSize());
        }

        // Tag filter: query file_tag table for matching file IDs
        if (dto.getTagIds() != null && !dto.getTagIds().isEmpty()) {
            Set<Long> fileIdsFromTags = getFileIdsByTags(dto.getTagIds(), dto.getTagLogic());
            if (fileIdsFromTags.isEmpty()) {
                return PageResult.of(0, 1, dto.getSize(), Collections.emptyList());
            }
            List<co.elastic.clients.elasticsearch._types.FieldValue> fvs = fileIdsFromTags.stream()
                    .map(id -> co.elastic.clients.elasticsearch._types.FieldValue.of(id))
                    .toList();
            boolBuilder.filter(Query.of(q -> q.terms(t -> t.field("id").terms(v -> v.value(fvs)))));
        }

        queryBuilder.withQuery(Query.of(q -> q.bool(boolBuilder.build())));

        // Highlight
        queryBuilder.withHighlightQuery(new HighlightQuery(
                new Highlight(List.of(new HighlightField("originalName"))), FileDocument.class));

        // Sort
        Sort sort = switch (dto.getSortBy()) {
            case "created_at" -> Sort.by("desc".equals(dto.getSortDir()) ? Sort.Direction.DESC : Sort.Direction.ASC, "createdAt");
            case "file_size" -> Sort.by("desc".equals(dto.getSortDir()) ? Sort.Direction.DESC : Sort.Direction.ASC, "fileSize");
            case "file_name" -> Sort.by("desc".equals(dto.getSortDir()) ? Sort.Direction.DESC : Sort.Direction.ASC, "originalName.keyword");
            default -> Sort.by(Sort.Direction.DESC, "_score");
        };

        queryBuilder.withPageable(PageRequest.of(dto.getPage() - 1, dto.getSize(), sort));

        SearchHits<FileDocument> hits = elasticsearchOperations.search(
                queryBuilder.build(), FileDocument.class, IndexCoordinates.of("teamfiles_files"));

        List<SearchResultVO> results = new ArrayList<>();
        for (SearchHit<FileDocument> hit : hits) {
            SearchResultVO vo = toResultVO(hit.getContent());
            vo.setHighlights(hit.getHighlightFields().entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue())));
            vo.setScore(hit.getScore());
            results.add(vo);
        }

        // Save search history
        if (StrUtil.isNotBlank(dto.getKeyword())) {
            saveSearchHistory(userId, dto.getKeyword(), results.size());
        }

        return PageResult.of(hits.getTotalHits(), dto.getPage(), dto.getSize(), results);
    }

    @Override
    public List<String> suggest(String keyword) {
        if (StrUtil.isBlank(keyword)) return Collections.emptyList();
        Set<String> suggestions = new LinkedHashSet<>();

        // From search history
        List<SearchHistory> history = searchHistoryMapper.selectList(
                new LambdaQueryWrapper<SearchHistory>()
                        .likeRight(SearchHistory::getKeyword, keyword)
                        .orderByDesc(SearchHistory::getCreatedAt)
                        .last("LIMIT 5"));
        for (SearchHistory h : history) {
            suggestions.add(h.getKeyword());
        }

        // From existing file names
        List<FileInfo> files = fileInfoMapper.selectList(
                new LambdaQueryWrapper<FileInfo>()
                        .likeRight(FileInfo::getOriginalName, keyword)
                        .eq(FileInfo::getDeleted, 0)
                        .last("LIMIT 5"));
        for (FileInfo f : files) {
            suggestions.add(f.getOriginalName());
        }

        return new ArrayList<>(suggestions).subList(0, Math.min(suggestions.size(), 10));
    }

    @Override
    public List<SearchHistory> getHistory() {
        Long userId = JwtContextHolder.getUserId();
        return searchHistoryMapper.selectList(
                new LambdaQueryWrapper<SearchHistory>()
                        .eq(SearchHistory::getUserId, userId)
                        .orderByDesc(SearchHistory::getCreatedAt)
                        .last("LIMIT 20"));
    }

    @Override
    @Transactional
    public void deleteHistory(Long id) {
        searchHistoryMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void clearHistory() {
        Long userId = JwtContextHolder.getUserId();
        searchHistoryMapper.delete(
                new LambdaQueryWrapper<SearchHistory>().eq(SearchHistory::getUserId, userId));
    }

    @Override
    @Transactional
    public SearchTemplate saveTemplate(SearchTemplateDTO dto) {
        Long userId = JwtContextHolder.getUserId();
        SearchTemplate template = new SearchTemplate();
        template.setUserId(userId);
        template.setTemplateName(dto.getTemplateName());
        template.setSearchCondition(dto.getSearchCondition());
        searchTemplateMapper.insert(template);
        return template;
    }

    @Override
    public List<SearchTemplate> getTemplates() {
        Long userId = JwtContextHolder.getUserId();
        return searchTemplateMapper.selectList(
                new LambdaQueryWrapper<SearchTemplate>()
                        .eq(SearchTemplate::getUserId, userId)
                        .orderByDesc(SearchTemplate::getCreatedAt));
    }

    @Override
    @Transactional
    public SearchTemplate updateTemplate(Long id, SearchTemplateDTO dto) {
        SearchTemplate template = searchTemplateMapper.selectById(id);
        if (template == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if (dto.getTemplateName() != null) template.setTemplateName(dto.getTemplateName());
        if (dto.getSearchCondition() != null) template.setSearchCondition(dto.getSearchCondition());
        searchTemplateMapper.updateById(template);
        return template;
    }

    @Override
    @Transactional
    public void deleteTemplate(Long id) {
        searchTemplateMapper.deleteById(id);
    }

    @Override
    public PageResult<SearchResultVO> executeTemplate(Long id) {
        SearchTemplate template = searchTemplateMapper.selectById(id);
        if (template == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        try {
            SearchDTO dto = new ObjectMapper().readValue(template.getSearchCondition(), SearchDTO.class);
            return search(dto);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Invalid search template");
        }
    }

    @Override
    public List<SearchResultVO> similarFiles(Long fileId) {
        FileInfo file = fileInfoMapper.selectById(fileId);
        if (file == null) {
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
        }

        // Find by same extension or similar name keywords
        NativeQuery query = NativeQuery.builder()
                .withQuery(Query.of(q -> q.bool(b -> b
                        .should(s -> s.term(t -> t.field("fileExtension").value(file.getFileExtension())))
                        .should(s -> s.match(m -> m.field("originalName").query(file.getOriginalName())))
                        .mustNot(mn -> mn.term(t -> t.field("id").value(file.getId())))
                        .minimumShouldMatch("1"))))
                .withPageable(PageRequest.of(0, 10))
                .build();

        SearchHits<FileDocument> hits = elasticsearchOperations.search(
                query, FileDocument.class, IndexCoordinates.of("teamfiles_files"));

        return hits.getSearchHits().stream()
                .map(h -> {
                    SearchResultVO vo = toResultVO(h.getContent());
                    vo.setScore(h.getScore());
                    return vo;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void rebuildIndex() {
        log.info("Starting full index rebuild...");
        List<FileInfo> files = fileInfoMapper.selectList(
                new LambdaQueryWrapper<FileInfo>().eq(FileInfo::getDeleted, 0));
        int count = 0;
        for (FileInfo file : files) {
            try {
                indexFile(toDocument(file));
                count++;
            } catch (Exception e) {
                log.warn("Failed to index file {}: {}", file.getId(), e.getMessage());
            }
        }
        log.info("Full index rebuild completed: {} files indexed", count);
    }

    @Override
    public void indexFile(FileDocument doc) {
        // Deferred to FileIndexSyncService
    }

    @Override
    public void deleteIndex(Long fileId) {
        // Deferred to FileIndexSyncService
    }

    private PageResult<SearchResultVO> mysqlSearch(SearchDTO dto, Long userId) {
        LambdaQueryWrapper<FileInfo> wrapper = new LambdaQueryWrapper<FileInfo>().eq(FileInfo::getDeleted, 0);
        if (StrUtil.isNotBlank(dto.getKeyword())) {
            wrapper.and(w -> w.like(FileInfo::getOriginalName, dto.getKeyword())
                    .or().like(FileInfo::getFileName, dto.getKeyword()));
        }
        if (StrUtil.isNotBlank(dto.getFileType())) {
            wrapper.eq(FileInfo::getFileType, dto.getFileType());
        }
        if (StrUtil.isNotBlank(dto.getStorageSpace())) {
            if ("PERSONAL".equals(dto.getStorageSpace())) {
                wrapper.eq(FileInfo::getUploadUserId, userId);
            } else {
                wrapper.eq(FileInfo::getStorageSpace, dto.getStorageSpace());
            }
        }
        if (dto.getTagIds() != null && !dto.getTagIds().isEmpty()) {
            Set<Long> fileIds = getFileIdsByTags(dto.getTagIds(), dto.getTagLogic());
            if (fileIds.isEmpty()) return PageResult.of(0, dto.getPage(), dto.getSize(), Collections.emptyList());
            wrapper.in(FileInfo::getId, fileIds);
        }
        wrapper.orderByDesc(FileInfo::getCreatedAt);
        Page<FileInfo> page = new Page<>(dto.getPage(), dto.getSize());
        IPage<FileInfo> result = fileInfoMapper.selectPage(page, wrapper);
        List<SearchResultVO> records = result.getRecords().stream().map(f -> {
            SearchResultVO vo = new SearchResultVO();
            vo.setFileId(f.getId()); vo.setOriginalName(f.getOriginalName());
            vo.setFileName(f.getFileName()); vo.setFileType(f.getFileType());
            vo.setFileExtension(f.getFileExtension()); vo.setFileSize(f.getFileSize());
            vo.setStorageSpace(f.getStorageSpace()); vo.setUploadUserId(f.getUploadUserId());
            User u = userMapper.selectById(f.getUploadUserId());
            if (u != null) vo.setUploadUsername(u.getNickname() != null ? u.getNickname() : u.getUsername());
            if (f.getCreatedAt() != null) vo.setCreatedAt(f.getCreatedAt().toString());
            if (StrUtil.isNotBlank(dto.getKeyword())) {
                String name = f.getOriginalName();
                String kw = dto.getKeyword();
                int idx = name.toLowerCase().indexOf(kw.toLowerCase());
                if (idx >= 0) {
                    String highlighted = name.substring(0, idx) + "<em>" + name.substring(idx, idx + kw.length()) + "</em>" + name.substring(idx + kw.length());
                    vo.setHighlights(Map.of("originalName", List.of(highlighted)));
                }
            }
            return vo;
        }).toList();
        saveSearchHistory(userId, dto.getKeyword(), records.size());
        return PageResult.of(result.getTotal(), dto.getPage(), dto.getSize(), records);
    }

    // --- helpers ---

    private void saveSearchHistory(Long userId, String keyword, int resultCount) {
        SearchHistory history = new SearchHistory();
        history.setUserId(userId);
        history.setKeyword(keyword);
        history.setSearchType("FILE");
        history.setResultCount(resultCount);
        searchHistoryMapper.insert(history);
    }

    private Set<Long> getFileIdsByTags(List<Long> tagIds, String logic) {
        List<FileTag> fileTags = fileTagMapper.selectList(
                new LambdaQueryWrapper<FileTag>().in(FileTag::getTagId, tagIds));

        Map<Long, Set<Long>> fileIdToTags = new HashMap<>();
        for (FileTag ft : fileTags) {
            fileIdToTags.computeIfAbsent(ft.getFileId(), k -> new HashSet<>()).add(ft.getTagId());
        }

        if ("OR".equalsIgnoreCase(logic)) {
            return fileIdToTags.keySet();
        }
        // AND: file must have ALL specified tags
        return fileIdToTags.entrySet().stream()
                .filter(e -> e.getValue().containsAll(tagIds))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
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

    private SearchResultVO toResultVO(FileDocument doc) {
        SearchResultVO vo = new SearchResultVO();
        vo.setFileId(doc.getId());
        vo.setOriginalName(doc.getOriginalName());
        vo.setFileName(doc.getFileName());
        vo.setFileType(doc.getFileType());
        vo.setFileExtension(doc.getFileExtension());
        vo.setFileSize(doc.getFileSize());
        vo.setStorageSpace(doc.getStorageSpace());
        vo.setUploadUserId(doc.getUploadUserId());
        vo.setUploadUsername(doc.getUploadUsername());
        if (doc.getCreatedAt() != null) {
            vo.setCreatedAt(doc.getCreatedAt().toString());
        }
        return vo;
    }
}
