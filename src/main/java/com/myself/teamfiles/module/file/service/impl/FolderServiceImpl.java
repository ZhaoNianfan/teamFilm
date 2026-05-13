package com.myself.teamfiles.module.file.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.myself.teamfiles.common.enums.StorageSpaceEnum;
import com.myself.teamfiles.common.exception.BusinessException;
import com.myself.teamfiles.common.exception.ErrorCode;
import com.myself.teamfiles.module.file.dto.FolderDTO;
import com.myself.teamfiles.module.file.entity.FileInfo;
import com.myself.teamfiles.module.file.entity.Folder;
import com.myself.teamfiles.module.file.mapper.FileInfoMapper;
import com.myself.teamfiles.module.file.mapper.FolderMapper;
import com.myself.teamfiles.module.file.service.FolderService;
import com.myself.teamfiles.module.recycle.entity.RecycleBin;
import com.myself.teamfiles.module.recycle.mapper.RecycleBinMapper;
import com.myself.teamfiles.security.JwtContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FolderServiceImpl implements FolderService {

    private final FolderMapper folderMapper;
    private final FileInfoMapper fileInfoMapper;
    private final RecycleBinMapper recycleBinMapper;

    @Override
    @Transactional
    public FolderDTO create(FolderDTO dto) {
        Long userId = JwtContextHolder.getUserId();
        String role = JwtContextHolder.getRole();

        if (dto.getParentId() != null && dto.getParentId() > 0) {
            Folder parent = folderMapper.selectById(dto.getParentId());
            if (parent == null || parent.getDeleted() == 1) {
                throw new BusinessException(ErrorCode.FOLDER_NOT_FOUND);
            }
            if (StorageSpaceEnum.PERSONAL.name().equals(parent.getStorageSpace())
                    && !parent.getOwnerUserId().equals(userId) && !"ADMIN".equals(role)) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "Cannot create folder under other user's personal folder");
            }
        }

        Folder folder = new Folder();
        folder.setFolderName(dto.getFolderName());
        folder.setParentId(dto.getParentId() != null ? dto.getParentId() : 0L);
        folder.setStorageSpace(dto.getStorageSpace());
        folder.setOwnerUserId(userId);
        folder.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
        folderMapper.insert(folder);
        return toDTO(folder);
    }

    @Override
    public List<FolderDTO> list(String storageSpace, Long parentId) {
        LambdaQueryWrapper<Folder> wrapper = new LambdaQueryWrapper<Folder>()
                .eq(Folder::getDeleted, 0)
                .orderByAsc(Folder::getSortOrder)
                .orderByDesc(Folder::getCreatedAt);

        if (storageSpace != null) {
            wrapper.eq(Folder::getStorageSpace, storageSpace);
        }
        if (parentId != null) {
            wrapper.eq(Folder::getParentId, parentId);
        }

        List<Folder> folders = folderMapper.selectList(wrapper);
        return folders.stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public FolderDTO getById(Long id) {
        Folder folder = folderMapper.selectById(id);
        if (folder == null || folder.getDeleted() == 1) {
            throw new BusinessException(ErrorCode.FOLDER_NOT_FOUND);
        }
        return toDTO(folder);
    }

    @Override
    @Transactional
    public void rename(Long id, String newName) {
        Folder folder = folderMapper.selectById(id);
        if (folder == null || folder.getDeleted() == 1) {
            throw new BusinessException(ErrorCode.FOLDER_NOT_FOUND);
        }
        checkFolderWriteAccess(folder);
        folder.setFolderName(newName);
        folderMapper.updateById(folder);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Folder folder = folderMapper.selectById(id);
        if (folder == null || folder.getDeleted() == 1) {
            throw new BusinessException(ErrorCode.FOLDER_NOT_FOUND);
        }
        checkFolderWriteAccess(folder);

        // Recursively collect all sub-folder and file IDs
        List<Long> folderIds = collectSubFolderIds(id);
        folderIds.add(id);

        // Move files in these folders to recycle bin
        List<FileInfo> files = fileInfoMapper.selectList(
                new LambdaQueryWrapper<FileInfo>().in(FileInfo::getFolderId, folderIds)
                        .eq(FileInfo::getDeleted, 0));
        for (FileInfo file : files) {
            RecycleBin rb = new RecycleBin();
            rb.setOriginalType("FILE");
            rb.setOriginalId(file.getId());
            rb.setFileName(file.getOriginalName());
            rb.setFilePath(file.getFilePath());
            rb.setFileSize(file.getFileSize());
            rb.setStorageSpace(file.getStorageSpace());
            rb.setOriginalParentId(file.getFolderId());
            rb.setDeletedBy(JwtContextHolder.getUserId());
            rb.setDeletedAt(LocalDateTime.now());
            rb.setExpireAt(LocalDateTime.now().plusDays(30));
            recycleBinMapper.insert(rb);
            fileInfoMapper.deleteById(file.getId());
        }

        // Move folder to recycle bin
        for (Long fid : folderIds) {
            Folder f = folderMapper.selectById(fid);
            if (f != null) {
                RecycleBin rb = new RecycleBin();
                rb.setOriginalType("FOLDER");
                rb.setOriginalId(f.getId());
                rb.setFileName(f.getFolderName());
                rb.setStorageSpace(f.getStorageSpace());
                rb.setOriginalParentId(f.getParentId());
                rb.setDeletedBy(JwtContextHolder.getUserId());
                rb.setDeletedAt(LocalDateTime.now());
                rb.setExpireAt(LocalDateTime.now().plusDays(30));
                recycleBinMapper.insert(rb);
                folderMapper.deleteById(fid);
            }
        }
    }

    @Override
    public List<FolderDTO> tree(String storageSpace) {
        Long userId = JwtContextHolder.getUserId();
        LambdaQueryWrapper<Folder> wrapper = new LambdaQueryWrapper<Folder>()
                .eq(Folder::getDeleted, 0)
                .orderByAsc(Folder::getSortOrder)
                .orderByDesc(Folder::getCreatedAt);

        if (StorageSpaceEnum.PERSONAL.name().equals(storageSpace)) {
            wrapper.eq(Folder::getStorageSpace, storageSpace)
                    .eq(Folder::getOwnerUserId, userId);
        } else if (StorageSpaceEnum.TEAM.name().equals(storageSpace)) {
            wrapper.eq(Folder::getStorageSpace, storageSpace);
        } else {
            wrapper.and(w -> w.eq(Folder::getStorageSpace, StorageSpaceEnum.TEAM.name())
                    .or().eq(Folder::getOwnerUserId, userId));
        }

        List<Folder> allFolders = folderMapper.selectList(wrapper);
        List<FolderDTO> dtos = allFolders.stream().map(this::toDTO).collect(Collectors.toList());

        // Count files per folder
        List<Long> folderIds = allFolders.stream().map(Folder::getId).collect(Collectors.toList());
        if (!folderIds.isEmpty()) {
            List<FileInfo> filesInFolders = fileInfoMapper.selectList(
                    new LambdaQueryWrapper<FileInfo>()
                            .in(FileInfo::getFolderId, folderIds)
                            .eq(FileInfo::getDeleted, 0));
            Map<Long, Long> countMap = filesInFolders.stream()
                    .collect(Collectors.groupingBy(f -> f.getFolderId() != null ? f.getFolderId() : 0L, Collectors.counting()));
            dtos.forEach(d -> d.setFileCount(countMap.getOrDefault(d.getId(), 0L).intValue()));
        }

        // Build tree
        Map<Long, List<FolderDTO>> childrenMap = dtos.stream()
                .filter(d -> d.getParentId() != null && d.getParentId() > 0)
                .collect(Collectors.groupingBy(FolderDTO::getParentId));

        List<FolderDTO> roots = new ArrayList<>();
        for (FolderDTO dto : dtos) {
            if (dto.getParentId() == null || dto.getParentId() == 0) {
                buildTree(dto, childrenMap);
                roots.add(dto);
            }
        }
        return roots;
    }

    private void buildTree(FolderDTO parent, Map<Long, List<FolderDTO>> childrenMap) {
        List<FolderDTO> children = childrenMap.getOrDefault(parent.getId(), new ArrayList<>());
        parent.setChildren(children);
        for (FolderDTO child : children) {
            buildTree(child, childrenMap);
        }
    }

    @Override
    @Transactional
    public void batchDelete(List<Long> ids) {
        for (Long id : ids) {
            delete(id);
        }
    }

    private List<Long> collectSubFolderIds(Long parentId) {
        List<Long> ids = new ArrayList<>();
        List<Folder> children = folderMapper.selectList(
                new LambdaQueryWrapper<Folder>().eq(Folder::getParentId, parentId)
                        .eq(Folder::getDeleted, 0));
        for (Folder child : children) {
            ids.add(child.getId());
            ids.addAll(collectSubFolderIds(child.getId()));
        }
        return ids;
    }

    private void checkFolderWriteAccess(Folder folder) {
        Long userId = JwtContextHolder.getUserId();
        String role = JwtContextHolder.getRole();
        if ("ADMIN".equals(role)) return;
        if (StorageSpaceEnum.PERSONAL.name().equals(folder.getStorageSpace())
                && !folder.getOwnerUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Cannot modify other user's personal folder");
        }
        if (StorageSpaceEnum.TEAM.name().equals(folder.getStorageSpace())
                && "GUEST".equals(role)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Guest users cannot modify team folders");
        }
    }

    private FolderDTO toDTO(Folder folder) {
        FolderDTO dto = new FolderDTO();
        dto.setId(folder.getId());
        dto.setFolderName(folder.getFolderName());
        dto.setParentId(folder.getParentId());
        dto.setStorageSpace(folder.getStorageSpace());
        dto.setOwnerUserId(folder.getOwnerUserId());
        dto.setSortOrder(folder.getSortOrder());
        dto.setCreatedAt(folder.getCreatedAt());
        return dto;
    }
}
