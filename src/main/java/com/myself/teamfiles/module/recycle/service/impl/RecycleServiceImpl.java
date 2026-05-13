package com.myself.teamfiles.module.recycle.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.myself.teamfiles.common.exception.BusinessException;
import com.myself.teamfiles.common.exception.ErrorCode;
import com.myself.teamfiles.common.result.PageResult;
import com.myself.teamfiles.module.file.entity.FileInfo;
import com.myself.teamfiles.module.file.entity.Folder;
import com.myself.teamfiles.module.file.mapper.FileInfoMapper;
import com.myself.teamfiles.module.file.mapper.FolderMapper;
import com.myself.teamfiles.module.recycle.dto.RecyclePageDTO;
import com.myself.teamfiles.module.recycle.dto.RecycleVO;
import com.myself.teamfiles.module.recycle.entity.RecycleBin;
import com.myself.teamfiles.module.recycle.mapper.RecycleBinMapper;
import com.myself.teamfiles.module.recycle.service.RecycleService;
import com.myself.teamfiles.module.user.entity.User;
import com.myself.teamfiles.module.user.mapper.UserMapper;
import com.myself.teamfiles.security.JwtContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecycleServiceImpl implements RecycleService {

    private final RecycleBinMapper recycleBinMapper;
    private final FileInfoMapper fileInfoMapper;
    private final FolderMapper folderMapper;
    private final UserMapper userMapper;

    @Value("${file.storage.base-path}")
    private String basePath;

    @Override
    public PageResult<RecycleVO> list(RecyclePageDTO dto) {
        Long userId = JwtContextHolder.getUserId();
        String role = JwtContextHolder.getRole();

        LambdaQueryWrapper<RecycleBin> wrapper = new LambdaQueryWrapper<RecycleBin>()
                .orderByDesc(RecycleBin::getDeletedAt);

        if ("ADMIN".equals(role) && StrUtil.isNotBlank(dto.getStorageSpace())) {
            wrapper.eq(RecycleBin::getStorageSpace, dto.getStorageSpace());
        } else if ("ADMIN".equals(role)) {
            // Admin sees all
        } else {
            wrapper.eq(RecycleBin::getDeletedBy, userId);
        }

        if (StrUtil.isNotBlank(dto.getKeyword())) {
            wrapper.like(RecycleBin::getFileName, dto.getKeyword());
        }

        Page<RecycleBin> page = new Page<>(dto.getPage(), dto.getSize());
        IPage<RecycleBin> result = recycleBinMapper.selectPage(page, wrapper);

        List<RecycleVO> records = result.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), records);
    }

    @Override
    @Transactional
    public void restore(Long id) {
        RecycleBin item = recycleBinMapper.selectById(id);
        if (item == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Recycle item not found");
        }

        // Bypass @TableLogic: use updateById to set deleted=0 without selecting first
        if ("FILE".equals(item.getOriginalType())) {
            FileInfo file = new FileInfo();
            file.setId(item.getOriginalId());
            file.setDeleted(0);
            fileInfoMapper.updateById(file);
        } else if ("FOLDER".equals(item.getOriginalType())) {
            Folder folder = new Folder();
            folder.setId(item.getOriginalId());
            folder.setDeleted(0);
            folderMapper.updateById(folder);
        }

        recycleBinMapper.deleteById(id);
        log.info("Restored recycle item: {} (type={})", item.getFileName(), item.getOriginalType());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        RecycleBin item = recycleBinMapper.selectById(id);
        if (item == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Recycle item not found");
        }
        permanentDelete(item);
    }

    @Override
    @Transactional
    public void batchDelete(List<Long> ids) {
        for (Long id : ids) {
            RecycleBin item = recycleBinMapper.selectById(id);
            if (item != null) {
                permanentDelete(item);
            }
        }
    }

    @Override
    @Transactional
    public void clear(String storageSpace) {
        Long userId = JwtContextHolder.getUserId();
        String role = JwtContextHolder.getRole();

        LambdaQueryWrapper<RecycleBin> wrapper = new LambdaQueryWrapper<>();
        if ("ADMIN".equals(role) && StrUtil.isNotBlank(storageSpace)) {
            wrapper.eq(RecycleBin::getStorageSpace, storageSpace);
        } else if ("ADMIN".equals(role)) {
            // Admin clears all
        } else {
            wrapper.eq(RecycleBin::getDeletedBy, userId);
            if (StrUtil.isNotBlank(storageSpace)) {
                wrapper.eq(RecycleBin::getStorageSpace, storageSpace);
            }
        }

        List<RecycleBin> items = recycleBinMapper.selectList(wrapper);
        for (RecycleBin item : items) {
            permanentDelete(item);
        }
        log.info("Cleared {} recycle items, storageSpace={}", items.size(), storageSpace);
    }

    private void permanentDelete(RecycleBin item) {
        if ("FILE".equals(item.getOriginalType())) {
            // Physically delete file
            if (item.getFilePath() != null) {
                try {
                    Files.deleteIfExists(Paths.get(basePath, item.getFilePath()));
                } catch (IOException e) {
                    log.warn("Failed to delete file: {}", item.getFilePath());
                }
            }
        }
        recycleBinMapper.deleteById(item.getId());
    }

    private RecycleVO toVO(RecycleBin item) {
        RecycleVO vo = new RecycleVO();
        vo.setId(item.getId());
        vo.setOriginalType(item.getOriginalType());
        vo.setOriginalId(item.getOriginalId());
        vo.setFileName(item.getFileName());
        vo.setFileSize(item.getFileSize());
        vo.setStorageSpace(item.getStorageSpace());
        vo.setOriginalParentId(item.getOriginalParentId());
        vo.setDeletedBy(item.getDeletedBy());
        vo.setDeletedAt(item.getDeletedAt());
        vo.setExpireAt(item.getExpireAt());

        if (item.getDeletedBy() != null) {
            User user = userMapper.selectById(item.getDeletedBy());
            if (user != null) {
                vo.setDeletedByName(user.getNickname() != null ? user.getNickname() : user.getUsername());
            }
        }
        if (item.getOriginalParentId() != null && item.getOriginalParentId() > 0) {
            Folder folder = folderMapper.selectById(item.getOriginalParentId());
            if (folder != null) {
                vo.setOriginalParentName(folder.getFolderName());
            }
        }
        return vo;
    }
}
