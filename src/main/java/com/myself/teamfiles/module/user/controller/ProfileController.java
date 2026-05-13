package com.myself.teamfiles.module.user.controller;

import com.myself.teamfiles.common.annotation.OperationLog;
import com.myself.teamfiles.common.result.R;
import com.myself.teamfiles.module.user.service.ProfileService;
import com.myself.teamfiles.security.JwtContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @OperationLog(module = "USER", operation = "Update nickname")
    @PutMapping("/nickname")
    public R<Void> updateNickname(@RequestBody Map<String, String> body) {
        Long userId = JwtContextHolder.getUserId();
        profileService.updateNickname(userId, body.get("nickname"));
        return R.ok();
    }

    @OperationLog(module = "USER", operation = "Upload avatar")
    @PutMapping("/avatar")
    public R<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        Long userId = JwtContextHolder.getUserId();
        return R.ok(profileService.uploadAvatar(userId, file));
    }

    @OperationLog(module = "USER", operation = "Change password")
    @PutMapping("/password")
    public R<Void> changePassword(@RequestBody Map<String, String> body) {
        Long userId = JwtContextHolder.getUserId();
        profileService.changePassword(userId, body.get("oldPassword"), body.get("newPassword"));
        return R.ok();
    }

    @GetMapping("/storage")
    public R<?> getStorageInfo() {
        Long userId = JwtContextHolder.getUserId();
        return R.ok(profileService.getStorageInfo(userId));
    }
}
