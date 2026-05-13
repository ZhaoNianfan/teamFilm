package com.myself.teamfiles.module.user.service.impl;

import com.myself.teamfiles.common.exception.BusinessException;
import com.myself.teamfiles.common.exception.ErrorCode;
import com.myself.teamfiles.module.user.entity.User;
import com.myself.teamfiles.module.user.mapper.UserMapper;
import com.myself.teamfiles.module.user.service.ProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Value("${file.storage.base-path}")
    private String basePath;

    @Override
    public void updateNickname(Long userId, String nickname) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        user.setNickname(nickname);
        userMapper.updateById(user);
    }

    @Override
    public String uploadAvatar(Long userId, MultipartFile file) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        try {
            Path avatarDir = Paths.get(basePath, "avatars");
            Files.createDirectories(avatarDir);

            String originalName = file.getOriginalFilename();
            String ext = "";
            if (originalName != null && originalName.contains(".")) {
                ext = originalName.substring(originalName.lastIndexOf("."));
            }
            String avatarName = UUID.randomUUID() + ext;
            Path avatarPath = avatarDir.resolve(avatarName);
            file.transferTo(avatarPath.toFile());

            String avatarUrl = "/avatars/" + avatarName;
            user.setAvatar(avatarUrl);
            userMapper.updateById(user);

            return avatarUrl;
        } catch (IOException e) {
            log.error("Failed to upload avatar", e);
            throw new BusinessException("Avatar upload failed");
        }
    }

    @Override
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_ERROR);
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
    }

    @Override
    public Map<String, Object> getStorageInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        Map<String, Object> info = new HashMap<>();
        info.put("totalQuota", user.getStorageQuota());

        // TODO: Calculate actual used storage from file_info table
        info.put("usedSpace", 0L);
        info.put("usagePercent", 0.0);

        return info;
    }
}
