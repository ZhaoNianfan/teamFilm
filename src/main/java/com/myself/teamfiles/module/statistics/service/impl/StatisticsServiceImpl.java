package com.myself.teamfiles.module.statistics.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.myself.teamfiles.module.file.entity.FileInfo;
import com.myself.teamfiles.module.file.entity.Folder;
import com.myself.teamfiles.module.file.mapper.FileInfoMapper;
import com.myself.teamfiles.module.file.mapper.FolderMapper;
import com.myself.teamfiles.module.recycle.entity.RecycleBin;
import com.myself.teamfiles.module.recycle.mapper.RecycleBinMapper;
import com.myself.teamfiles.module.statistics.dto.AdminStatsVO;
import com.myself.teamfiles.module.statistics.dto.DashboardVO;
import com.myself.teamfiles.module.statistics.service.StatisticsService;
import com.myself.teamfiles.module.tag.entity.FileTag;
import com.myself.teamfiles.module.tag.entity.Tag;
import com.myself.teamfiles.module.tag.mapper.FileTagMapper;
import com.myself.teamfiles.module.tag.mapper.TagMapper;
import com.myself.teamfiles.module.user.entity.User;
import com.myself.teamfiles.module.user.mapper.UserMapper;
import com.myself.teamfiles.security.JwtContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final FileInfoMapper fileInfoMapper;
    private final FolderMapper folderMapper;
    private final UserMapper userMapper;
    private final TagMapper tagMapper;
    private final FileTagMapper fileTagMapper;
    private final RecycleBinMapper recycleBinMapper;

    @Override
    public DashboardVO dashboard(String space) {
        Long userId = JwtContextHolder.getUserId();
        String role = JwtContextHolder.getRole();
        boolean isAdmin = "ADMIN".equals(role);
        boolean showAll = isAdmin || "TEAM".equals(space);
        DashboardVO vo = new DashboardVO();

        LambdaQueryWrapper<FileInfo> fileWrapper = new LambdaQueryWrapper<FileInfo>().eq(FileInfo::getDeleted, 0);
        LambdaQueryWrapper<Folder> folderWrapper = new LambdaQueryWrapper<Folder>().eq(Folder::getDeleted, 0);

        if (showAll && isAdmin) {
            // Admin: show all files in system by default
        } else if ("TEAM".equals(space)) {
            // Non-admin team view
            fileWrapper.eq(FileInfo::getStorageSpace, "TEAM");
            folderWrapper.eq(Folder::getStorageSpace, "TEAM");
        } else {
            // Personal view
            fileWrapper.eq(FileInfo::getUploadUserId, userId);
            folderWrapper.eq(Folder::getOwnerUserId, userId);
        }

        // Storage (quota only relevant for personal)
        User user = userMapper.selectById(userId);
        DashboardVO.StorageInfo storage = new DashboardVO.StorageInfo();
        if (user != null && user.getStorageQuota() != null) {
            storage.setQuota(user.getStorageQuota());
            if (showAll && isAdmin) {
                List<FileInfo> all = fileInfoMapper.selectList(new LambdaQueryWrapper<FileInfo>().eq(FileInfo::getDeleted, 0));
                storage.setUsed(all.stream().mapToLong(FileInfo::getFileSize).sum());
            } else {
                List<FileInfo> personals = fileInfoMapper.selectList(
                        new LambdaQueryWrapper<FileInfo>().eq(FileInfo::getUploadUserId, userId)
                                .eq(FileInfo::getStorageSpace, "PERSONAL").eq(FileInfo::getDeleted, 0));
                storage.setUsed(personals.stream().mapToLong(FileInfo::getFileSize).sum());
            }
            storage.setPercentage(storage.getQuota() > 0 ? Math.round(storage.getUsed() * 10000.0 / storage.getQuota()) / 100.0 : 0);
        }
        vo.setStorage(storage);

        // Total files & folders
        vo.setTotalFiles(fileInfoMapper.selectCount(fileWrapper));
        vo.setTotalFolders(folderMapper.selectCount(folderWrapper));

        // File type distribution
        List<FileInfo> distFiles = fileInfoMapper.selectList(fileWrapper);
        Map<String, DashboardVO.FileTypeStat> typeStats = new LinkedHashMap<>();
        for (FileInfo f : distFiles) {
            typeStats.computeIfAbsent(f.getFileType(), k -> {
                DashboardVO.FileTypeStat s = new DashboardVO.FileTypeStat();
                s.setType(k);
                return s;
            });
            DashboardVO.FileTypeStat st = typeStats.get(f.getFileType());
            st.setCount(st.getCount() + 1);
            st.setTotalSize(st.getTotalSize() + f.getFileSize());
        }
        vo.setFileTypeDistribution(new ArrayList<>(typeStats.values()));

        // Recent files (top 10)
        LambdaQueryWrapper<FileInfo> recentWrapper = new LambdaQueryWrapper<FileInfo>().eq(FileInfo::getDeleted, 0);
        if (!showAll || !isAdmin) {
            if ("TEAM".equals(space)) recentWrapper.eq(FileInfo::getStorageSpace, "TEAM");
            else recentWrapper.eq(FileInfo::getUploadUserId, userId);
        }
        List<FileInfo> recentFiles = fileInfoMapper.selectList(
                recentWrapper.orderByDesc(FileInfo::getCreatedAt).last("LIMIT 10"));
        vo.setRecentFiles(recentFiles.stream().map(f -> {
            DashboardVO.RecentFile rf = new DashboardVO.RecentFile();
            rf.setId(f.getId()); rf.setName(f.getOriginalName()); rf.setType(f.getFileType());
            User u = userMapper.selectById(f.getUploadUserId());
            rf.setUploader(u != null ? (u.getNickname() != null ? u.getNickname() : u.getUsername()) : "");
            rf.setCreatedAt(f.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            return rf;
        }).toList());

        // Hot tags
        List<FileTag> allFileTags = fileTagMapper.selectList(new LambdaQueryWrapper<>());
        Map<Long, Long> tagCounts = allFileTags.stream()
                .collect(Collectors.groupingBy(FileTag::getTagId, Collectors.counting()));
        List<Long> topTagIds = tagCounts.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed()).limit(20).map(Map.Entry::getKey).toList();
        if (!topTagIds.isEmpty()) {
            List<Tag> hotTags = tagMapper.selectBatchIds(topTagIds);
            vo.setHotTags(hotTags.stream().map(t -> {
                DashboardVO.TagStat ts = new DashboardVO.TagStat();
                ts.setId(t.getId()); ts.setName(t.getTagName()); ts.setColor(t.getColor());
                ts.setCount(tagCounts.getOrDefault(t.getId(), 0L));
                return ts;
            }).sorted((a, b) -> Long.compare(b.getCount(), a.getCount())).toList());
        } else {
            vo.setHotTags(Collections.emptyList());
        }

        // Today uploads
        LambdaQueryWrapper<FileInfo> todayWrapper = new LambdaQueryWrapper<FileInfo>()
                .ge(FileInfo::getCreatedAt, LocalDate.now().atStartOfDay()).eq(FileInfo::getDeleted, 0);
        if (!showAll || !isAdmin) {
            if ("TEAM".equals(space)) todayWrapper.eq(FileInfo::getStorageSpace, "TEAM");
            else todayWrapper.eq(FileInfo::getUploadUserId, userId);
        }
        vo.setTodayUploads(fileInfoMapper.selectCount(todayWrapper));

        return vo;
    }

    @Override
    public AdminStatsVO adminOverview() {
        AdminStatsVO vo = new AdminStatsVO();

        vo.setTotalUsers(userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getDeleted, 0)));
        vo.setActiveUsers(userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getStatus, 1).eq(User::getDeleted, 0)));
        vo.setTotalFiles(fileInfoMapper.selectCount(
                new LambdaQueryWrapper<FileInfo>().eq(FileInfo::getDeleted, 0)));
        vo.setTotalFolders(folderMapper.selectCount(
                new LambdaQueryWrapper<Folder>().eq(Folder::getDeleted, 0)));
        vo.setTeamFiles(fileInfoMapper.selectCount(
                new LambdaQueryWrapper<FileInfo>().eq(FileInfo::getStorageSpace, "TEAM").eq(FileInfo::getDeleted, 0)));
        vo.setPersonalFiles(fileInfoMapper.selectCount(
                new LambdaQueryWrapper<FileInfo>().eq(FileInfo::getStorageSpace, "PERSONAL").eq(FileInfo::getDeleted, 0)));

        List<FileInfo> allFiles = fileInfoMapper.selectList(
                new LambdaQueryWrapper<FileInfo>().eq(FileInfo::getDeleted, 0));
        vo.setTotalStorageUsed(allFiles.stream().mapToLong(FileInfo::getFileSize).sum());

        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        vo.setTodayUploads(fileInfoMapper.selectCount(
                new LambdaQueryWrapper<FileInfo>().ge(FileInfo::getCreatedAt, todayStart).eq(FileInfo::getDeleted, 0)));
        vo.setTodayDownloads(allFiles.stream()
                .filter(f -> f.getDownloadCount() != null && f.getDownloadCount() > 0)
                .mapToInt(FileInfo::getDownloadCount).sum());

        vo.setRecycleItems(recycleBinMapper.selectCount(new LambdaQueryWrapper<>()));

        return vo;
    }
}
