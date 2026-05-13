package com.myself.teamfiles.module.file.service;

import com.myself.teamfiles.module.file.dto.FolderDTO;

import java.util.List;

public interface FolderService {
    FolderDTO create(FolderDTO dto);
    List<FolderDTO> list(String storageSpace, Long parentId);
    FolderDTO getById(Long id);
    void rename(Long id, String newName);
    void delete(Long id);
    List<FolderDTO> tree(String storageSpace);
    void batchDelete(List<Long> ids);
}
