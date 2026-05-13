package com.myself.teamfiles.module.user.controller;

import com.myself.teamfiles.common.annotation.OperationLog;
import com.myself.teamfiles.common.result.R;
import com.myself.teamfiles.module.user.dto.CreateUserDTO;
import com.myself.teamfiles.module.user.dto.UserPageDTO;
import com.myself.teamfiles.module.user.service.UserService;
import com.myself.teamfiles.security.JwtContextHolder;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @OperationLog(module = "USER", operation = "Create user")
    @PostMapping
    public R<?> createUser(@Valid @RequestBody CreateUserDTO dto) {
        Long createdBy = JwtContextHolder.getUserId();
        return R.ok(userService.createUser(dto, createdBy));
    }

    @GetMapping
    public R<?> getUserList(@Valid UserPageDTO dto) {
        return R.ok(userService.getUserList(dto));
    }

    @GetMapping("/{id}")
    public R<?> getUserById(@PathVariable Long id) {
        return R.ok(userService.getUserById(id));
    }

    @OperationLog(module = "USER", operation = "Update user status")
    @PutMapping("/{id}/status")
    public R<Void> updateStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        userService.updateUserStatus(id, body.get("status"));
        return R.ok();
    }

    @OperationLog(module = "USER", operation = "Update user")
    @PutMapping("/{id}")
    public R<Void> updateUser(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String nickname = body.containsKey("nickname") ? (String) body.get("nickname") : null;
        String role = body.containsKey("role") ? (String) body.get("role") : null;
        Integer status = body.containsKey("status") ? ((Number) body.get("status")).intValue() : null;
        Long storageQuota = body.containsKey("storageQuota") ? ((Number) body.get("storageQuota")).longValue() : null;
        userService.updateUser(id, nickname, role, status, storageQuota);
        return R.ok();
    }

    @OperationLog(module = "USER", operation = "Delete user")
    @DeleteMapping("/{id}")
    public R<Void> deleteUser(@PathVariable Long id,
                              @RequestParam(defaultValue = "DELETE") String fileHandle) {
        userService.deleteUser(id, fileHandle);
        return R.ok();
    }

    @OperationLog(module = "USER", operation = "Reset user password")
    @PutMapping("/{id}/reset-password")
    public R<Void> resetPassword(@PathVariable Long id, @RequestBody Map<String, String> body) {
        userService.resetPassword(id, body.get("password"));
        return R.ok();
    }
}
