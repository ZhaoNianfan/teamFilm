package com.myself.teamfiles.module.tag.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.myself.teamfiles.common.enums.StorageSpaceEnum;
import com.myself.teamfiles.common.exception.BusinessException;
import com.myself.teamfiles.common.exception.ErrorCode;
import com.myself.teamfiles.module.file.dto.FileVO;
import com.myself.teamfiles.module.file.entity.FileInfo;
import com.myself.teamfiles.module.file.entity.Folder;
import com.myself.teamfiles.module.file.mapper.FileInfoMapper;
import com.myself.teamfiles.module.file.mapper.FolderMapper;
import com.myself.teamfiles.module.tag.dto.*;
import com.myself.teamfiles.module.tag.entity.*;
import com.myself.teamfiles.module.tag.mapper.*;
import com.myself.teamfiles.module.tag.service.TagService;
import com.myself.teamfiles.module.user.entity.User;
import com.myself.teamfiles.module.user.mapper.UserMapper;
import com.myself.teamfiles.security.JwtContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {

    private final TagMapper tagMapper;
    private final FileTagMapper fileTagMapper;
    private final UserTagVisibleMapper userTagVisibleMapper;
    private final TagGroupMapper tagGroupMapper;
    private final TagGroupItemMapper tagGroupItemMapper;
    private final FileInfoMapper fileInfoMapper;
    private final FolderMapper folderMapper;
    private final UserMapper userMapper;

    // ==================== Tag CRUD ====================

    @Override
    @Transactional
    public TagVO create(TagDTO dto) {
        Long userId = JwtContextHolder.getUserId();

        // Check duplicate name
        Tag existing = tagMapper.selectOne(
                new LambdaQueryWrapper<Tag>().eq(Tag::getTagName, dto.getTagName())
                        .eq(Tag::getDeleted, 0));
        if (existing != null) {
            // Tag exists, add visibility for this user
            addVisibility(userId, existing.getId());
            return toVO(existing);
        }

        Tag tag = new Tag();
        tag.setTagName(dto.getTagName());
        tag.setColor(dto.getColor() != null ? dto.getColor() : "#409EFF");
        tag.setCreatorUserId(userId);
        tag.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
        tagMapper.insert(tag);

        // Add visibility for creator
        addVisibility(userId, tag.getId());

        return toVO(tag);
    }

    @Override
    public List<TagVO> listVisible() {
        Long userId = JwtContextHolder.getUserId();
        List<UserTagVisible> visibles = userTagVisibleMapper.selectList(
                new LambdaQueryWrapper<UserTagVisible>().eq(UserTagVisible::getUserId, userId));
        if (visibles.isEmpty()) return Collections.emptyList();

        List<Long> tagIds = visibles.stream().map(UserTagVisible::getTagId).toList();
        List<Tag> tags = tagMapper.selectBatchIds(tagIds);
        // Filter out deleted tags
        tags = tags.stream().filter(t -> t.getDeleted() == 0).collect(Collectors.toList());

        // Count files per tag (total, personal, shared)
        Map<Long, Integer> totalMap = new HashMap<>();
        Map<Long, Integer> personalMap = new HashMap<>();
        Map<Long, Integer> sharedMap = new HashMap<>();
        if (!tagIds.isEmpty()) {
            List<FileTag> fileTags = fileTagMapper.selectList(
                    new LambdaQueryWrapper<FileTag>().in(FileTag::getTagId, tagIds));
            Set<Long> fileIds = fileTags.stream().map(FileTag::getFileId).collect(Collectors.toSet());
            Map<Long, FileInfo> fileMap = new HashMap<>();
            if (!fileIds.isEmpty()) {
                fileInfoMapper.selectBatchIds(fileIds).forEach(f -> fileMap.put(f.getId(), f));
            }
            for (FileTag ft : fileTags) {
                totalMap.merge(ft.getTagId(), 1, Integer::sum);
                FileInfo fi = fileMap.get(ft.getFileId());
                if (fi != null && fi.getDeleted() == 0) {
                    if (StorageSpaceEnum.TEAM.name().equals(fi.getStorageSpace())) {
                        sharedMap.merge(ft.getTagId(), 1, Integer::sum);
                    } else if (fi.getUploadUserId() != null && fi.getUploadUserId().equals(userId)) {
                        personalMap.merge(ft.getTagId(), 1, Integer::sum);
                    }
                }
            }
        }

        return tags.stream()
                .sorted(Comparator.comparingInt(Tag::getSortOrder).thenComparing(Tag::getCreatedAt).reversed())
                .map(t -> toVO(t, totalMap.getOrDefault(t.getId(), 0), personalMap.getOrDefault(t.getId(), 0), sharedMap.getOrDefault(t.getId(), 0)))
                .collect(Collectors.toList());
    }

    @Override
    public TagVO getById(Long id) {
        Tag tag = tagMapper.selectById(id);
        if (tag == null || tag.getDeleted() == 1) {
            throw new BusinessException(ErrorCode.TAG_NOT_FOUND);
        }
        Long count = fileTagMapper.selectCount(
                new LambdaQueryWrapper<FileTag>().eq(FileTag::getTagId, id));
        return toVO(tag, count.intValue());
    }

    @Override
    @Transactional
    public TagVO update(Long id, TagDTO dto) {
        Tag tag = tagMapper.selectById(id);
        if (tag == null || tag.getDeleted() == 1) {
            throw new BusinessException(ErrorCode.TAG_NOT_FOUND);
        }
        // Only creator or admin can edit
        Long userId = JwtContextHolder.getUserId();
        String role = JwtContextHolder.getRole();
        if (!"ADMIN".equals(role) && (tag.getCreatorUserId() == null || !tag.getCreatorUserId().equals(userId))) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只有标签创建者或管理员可以编辑标签");
        }
        if (dto.getTagName() != null) tag.setTagName(dto.getTagName());
        if (dto.getColor() != null) tag.setColor(dto.getColor());
        if (dto.getSortOrder() != null) tag.setSortOrder(dto.getSortOrder());
        tagMapper.updateById(tag);
        return toVO(tag);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Tag tag = tagMapper.selectById(id);
        if (tag == null || tag.getDeleted() == 1) {
            throw new BusinessException(ErrorCode.TAG_NOT_FOUND);
        }
        Long userId = JwtContextHolder.getUserId();

        // Weak delete: remove current user's visibility only
        userTagVisibleMapper.delete(
                new LambdaQueryWrapper<UserTagVisible>()
                        .eq(UserTagVisible::getUserId, userId)
                        .eq(UserTagVisible::getTagId, id));

        // If no one else can see this tag, soft delete it
        Long visibleCount = userTagVisibleMapper.selectCount(
                new LambdaQueryWrapper<UserTagVisible>().eq(UserTagVisible::getTagId, id));
        if (visibleCount == 0) {
            tagMapper.deleteById(id);
        }
    }

    // ==================== File-Tag Association ====================

    @Override
    @Transactional
    public void addTagsToFile(Long fileId, List<Long> tagIds) {
        FileInfo file = fileInfoMapper.selectById(fileId);
        if (file == null || file.getDeleted() == 1) {
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
        }
        Long userId = JwtContextHolder.getUserId();

        for (Long tagId : tagIds) {
            // Ensure tag exists
            Tag tag = tagMapper.selectById(tagId);
            if (tag == null || tag.getDeleted() == 1) continue;

            // Ensure visibility
            addVisibility(userId, tagId);

            // Add file-tag association if not exists
            FileTag existing = fileTagMapper.selectOne(
                    new LambdaQueryWrapper<FileTag>()
                            .eq(FileTag::getFileId, fileId)
                            .eq(FileTag::getTagId, tagId));
            if (existing == null) {
                FileTag ft = new FileTag();
                ft.setFileId(fileId);
                ft.setTagId(tagId);
                ft.setCreatedBy(userId);
                fileTagMapper.insert(ft);
            }
        }
    }

    @Override
    @Transactional
    public void removeTagFromFile(Long fileId, Long tagId) {
        fileTagMapper.delete(
                new LambdaQueryWrapper<FileTag>()
                        .eq(FileTag::getFileId, fileId)
                        .eq(FileTag::getTagId, tagId));
    }

    @Override
    @Transactional
    public void batchTags(BatchTagsDTO dto) {
        for (Long fileId : dto.getFileIds()) {
            if (dto.isAdd()) {
                addTagsToFile(fileId, dto.getTagIds());
            } else {
                for (Long tagId : dto.getTagIds()) {
                    removeTagFromFile(fileId, tagId);
                }
            }
        }
    }

    @Override
    public List<FileVO> getFilesByTag(Long tagId) {
        Long userId = JwtContextHolder.getUserId();
        List<FileTag> fileTags = fileTagMapper.selectList(
                new LambdaQueryWrapper<FileTag>().eq(FileTag::getTagId, tagId));
        if (fileTags.isEmpty()) return Collections.emptyList();

        List<Long> fileIds = fileTags.stream().map(FileTag::getFileId).toList();
        List<FileInfo> files = fileInfoMapper.selectBatchIds(fileIds);
        files = files.stream()
                .filter(f -> f.getDeleted() == 0)
                .filter(f -> f.getUploadUserId() != null && f.getUploadUserId().equals(userId)
                        || StorageSpaceEnum.TEAM.name().equals(f.getStorageSpace()))
                .collect(Collectors.toList());

        return files.stream().map(this::fileToVO).collect(Collectors.toList());
    }

    // ==================== Search & Suggestion ====================

    @Override
    public List<TagVO> autocomplete(String keyword) {
        if (keyword == null || keyword.isBlank()) return Collections.emptyList();
        List<Tag> tags = tagMapper.selectList(
                new LambdaQueryWrapper<Tag>()
                        .likeRight(Tag::getTagName, keyword)
                        .eq(Tag::getDeleted, 0)
                        .last("LIMIT 10"));
        return tags.stream().map(t -> toVO(t, 0)).collect(Collectors.toList());
    }

    @Override
    public List<TagVO> similar(String keyword) {
        if (keyword == null || keyword.isBlank()) return Collections.emptyList();
        List<Tag> allTags = tagMapper.selectList(
                new LambdaQueryWrapper<Tag>().eq(Tag::getDeleted, 0));
        return allTags.stream()
                .filter(t -> levenshteinDistance(keyword.toLowerCase(), t.getTagName().toLowerCase()) <= 2)
                .map(t -> toVO(t, 0))
                .collect(Collectors.toList());
    }

    private int levenshteinDistance(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= b.length(); j++) dp[0][j] = j;
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost);
            }
        }
        return dp[a.length()][b.length()];
    }

    // ==================== Tag Groups ====================

    @Override
    @Transactional
    public TagGroupDTO createGroup(TagGroupDTO dto) {
        Long userId = JwtContextHolder.getUserId();
        TagGroup group = new TagGroup();
        group.setGroupName(dto.getGroupName());
        group.setOwnerUserId(userId);
        group.setColor(dto.getColor() != null ? dto.getColor() : "#409EFF");
        group.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
        tagGroupMapper.insert(group);
        return toGroupDTO(group);
    }

    @Override
    public List<TagGroupDTO> listGroups() {
        Long userId = JwtContextHolder.getUserId();
        List<TagGroup> groups = tagGroupMapper.selectList(
                new LambdaQueryWrapper<TagGroup>()
                        .eq(TagGroup::getOwnerUserId, userId)
                        .eq(TagGroup::getDeleted, 0)
                        .orderByAsc(TagGroup::getSortOrder));
        return groups.stream().map(this::toGroupDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TagGroupDTO updateGroup(Long id, TagGroupDTO dto) {
        TagGroup group = tagGroupMapper.selectById(id);
        if (group == null || group.getDeleted() == 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if (dto.getGroupName() != null) group.setGroupName(dto.getGroupName());
        if (dto.getColor() != null) group.setColor(dto.getColor());
        if (dto.getSortOrder() != null) group.setSortOrder(dto.getSortOrder());
        tagGroupMapper.updateById(group);
        return toGroupDTO(group);
    }

    @Override
    @Transactional
    public void deleteGroup(Long id) {
        TagGroup group = tagGroupMapper.selectById(id);
        if (group == null || group.getDeleted() == 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        // Remove group items, keep tags
        tagGroupItemMapper.delete(
                new LambdaQueryWrapper<TagGroupItem>().eq(TagGroupItem::getGroupId, id));
        // Soft delete group
        tagGroupMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void addTagsToGroup(Long groupId, List<Long> tagIds) {
        TagGroup group = tagGroupMapper.selectById(groupId);
        if (group == null || group.getDeleted() == 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        for (Long tagId : tagIds) {
            TagGroupItem existing = tagGroupItemMapper.selectOne(
                    new LambdaQueryWrapper<TagGroupItem>()
                            .eq(TagGroupItem::getGroupId, groupId)
                            .eq(TagGroupItem::getTagId, tagId));
            if (existing == null) {
                TagGroupItem item = new TagGroupItem();
                item.setGroupId(groupId);
                item.setTagId(tagId);
                tagGroupItemMapper.insert(item);
            }
        }
    }

    @Override
    @Transactional
    public void removeTagFromGroup(Long groupId, Long tagId) {
        tagGroupItemMapper.delete(
                new LambdaQueryWrapper<TagGroupItem>()
                        .eq(TagGroupItem::getGroupId, groupId)
                        .eq(TagGroupItem::getTagId, tagId));
    }

    // ==================== Helpers ====================

    private void addVisibility(Long userId, Long tagId) {
        UserTagVisible existing = userTagVisibleMapper.selectOne(
                new LambdaQueryWrapper<UserTagVisible>()
                        .eq(UserTagVisible::getUserId, userId)
                        .eq(UserTagVisible::getTagId, tagId));
        if (existing == null) {
            UserTagVisible v = new UserTagVisible();
            v.setUserId(userId);
            v.setTagId(tagId);
            userTagVisibleMapper.insert(v);
        }
    }

    private TagVO toVO(Tag tag) {
        return toVO(tag, 0, 0, 0);
    }

    private TagVO toVO(Tag tag, int fileCount) {
        return toVO(tag, fileCount, 0, 0);
    }

    private TagVO toVO(Tag tag, int fileCount, int personalCount, int sharedCount) {
        TagVO vo = new TagVO();
        vo.setId(tag.getId());
        vo.setTagName(tag.getTagName());
        vo.setColor(tag.getColor());
        vo.setCreatorUserId(tag.getCreatorUserId());
        vo.setSortOrder(tag.getSortOrder());
        vo.setFileCount(fileCount);
        vo.setPersonalFileCount(personalCount);
        vo.setSharedFileCount(sharedCount);
        vo.setCreatedAt(tag.getCreatedAt());
        if (tag.getCreatorUserId() != null) {
            User user = userMapper.selectById(tag.getCreatorUserId());
            if (user != null) {
                vo.setCreatorUsername(user.getNickname() != null ? user.getNickname() : user.getUsername());
            }
        }
        return vo;
    }

    private TagGroupDTO toGroupDTO(TagGroup group) {
        TagGroupDTO dto = new TagGroupDTO();
        dto.setId(group.getId());
        dto.setGroupName(group.getGroupName());
        dto.setColor(group.getColor());
        dto.setSortOrder(group.getSortOrder());

        // Load tags in group
        List<TagGroupItem> items = tagGroupItemMapper.selectList(
                new LambdaQueryWrapper<TagGroupItem>().eq(TagGroupItem::getGroupId, group.getId()));
        if (!items.isEmpty()) {
            List<Long> tagIds = items.stream().map(TagGroupItem::getTagId).toList();
            List<Tag> tags = tagMapper.selectBatchIds(tagIds);
            tags = tags.stream().filter(t -> t.getDeleted() == 0).collect(Collectors.toList());
            final Map<Long, Long> countMap;
            if (!tagIds.isEmpty()) {
                List<FileTag> fileTags = fileTagMapper.selectList(
                        new LambdaQueryWrapper<FileTag>().in(FileTag::getTagId, tagIds));
                countMap = fileTags.stream()
                        .collect(Collectors.groupingBy(FileTag::getTagId, Collectors.counting()));
            } else {
                countMap = Collections.emptyMap();
            }
            dto.setTags(tags.stream()
                    .map(t -> toVO(t, ((Long) countMap.getOrDefault(t.getId(), 0L)).intValue()))
                    .collect(Collectors.toList()));
            dto.setTagCount(tags.size());
        } else {
            dto.setTags(Collections.emptyList());
            dto.setTagCount(0);
        }
        return dto;
    }

    private FileVO fileToVO(FileInfo f) {
        FileVO vo = new FileVO();
        vo.setId(f.getId());
        vo.setFileName(f.getFileName());
        vo.setOriginalName(f.getOriginalName());
        vo.setFileSize(f.getFileSize());
        vo.setFileType(f.getFileType());
        vo.setMimeType(f.getMimeType());
        vo.setFileExtension(f.getFileExtension());
        vo.setMd5(f.getMd5());
        vo.setStorageSpace(f.getStorageSpace());
        vo.setFolderId(f.getFolderId());
        vo.setUploadUserId(f.getUploadUserId());
        vo.setCreatedAt(f.getCreatedAt());
        vo.setUpdatedAt(f.getUpdatedAt());
        if (f.getFolderId() != null && f.getFolderId() > 0) {
            Folder folder = folderMapper.selectById(f.getFolderId());
            if (folder != null) vo.setFolderName(folder.getFolderName());
        }
        if (f.getUploadUserId() != null) {
            User user = userMapper.selectById(f.getUploadUserId());
            if (user != null) vo.setUploadUsername(user.getNickname() != null ? user.getNickname() : user.getUsername());
        }
        return vo;
    }
}
