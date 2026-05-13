package com.myself.teamfiles.module.tag.service;

import com.myself.teamfiles.module.file.dto.FileVO;
import com.myself.teamfiles.module.tag.dto.*;

import java.util.List;

public interface TagService {
    // Tag CRUD
    TagVO create(TagDTO dto);
    List<TagVO> listVisible();
    TagVO getById(Long id);
    TagVO update(Long id, TagDTO dto);
    void delete(Long id);

    // File-tag association
    void addTagsToFile(Long fileId, List<Long> tagIds);
    void removeTagFromFile(Long fileId, Long tagId);
    void batchTags(BatchTagsDTO dto);
    List<FileVO> getFilesByTag(Long tagId);

    // Search & suggestion
    List<TagVO> autocomplete(String keyword);
    List<TagVO> similar(String keyword);

    // Tag groups
    TagGroupDTO createGroup(TagGroupDTO dto);
    List<TagGroupDTO> listGroups();
    TagGroupDTO updateGroup(Long id, TagGroupDTO dto);
    void deleteGroup(Long id);
    void addTagsToGroup(Long groupId, List<Long> tagIds);
    void removeTagFromGroup(Long groupId, Long tagId);
}
