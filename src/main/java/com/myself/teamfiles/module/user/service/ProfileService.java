package com.myself.teamfiles.module.user.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface ProfileService {
    void updateNickname(Long userId, String nickname);
    String uploadAvatar(Long userId, MultipartFile file);
    void changePassword(Long userId, String oldPassword, String newPassword);
    Map<String, Object> getStorageInfo(Long userId);
}
