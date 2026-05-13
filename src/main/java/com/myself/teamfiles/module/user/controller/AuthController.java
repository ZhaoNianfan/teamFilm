package com.myself.teamfiles.module.user.controller;

import com.myself.teamfiles.common.annotation.OperationLog;
import com.myself.teamfiles.common.result.R;
import com.myself.teamfiles.module.user.dto.ChangePasswordDTO;
import com.myself.teamfiles.module.user.dto.LoginDTO;
import com.myself.teamfiles.module.user.service.AuthService;
import com.myself.teamfiles.security.JwtContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @OperationLog(module = "AUTH", operation = "User login")
    @PostMapping("/login")
    public R<?> login(@Valid @RequestBody LoginDTO loginDTO, HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        return R.ok(authService.login(loginDTO, ip));
    }

    @OperationLog(module = "AUTH", operation = "User logout")
    @PostMapping("/logout")
    public R<Void> logout(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        authService.logout(token);
        return R.ok();
    }

    @PostMapping("/refresh")
    public R<String> refresh(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        return R.ok(authService.refreshToken(token));
    }

    @GetMapping("/me")
    public R<?> currentUser() {
        Long userId = JwtContextHolder.getUserId();
        return R.ok(authService.getCurrentUser(userId));
    }

    @OperationLog(module = "AUTH", operation = "First login password change")
    @PutMapping("/password")
    public R<Void> changePasswordOnFirstLogin(@Valid @RequestBody ChangePasswordDTO dto) {
        authService.changePasswordOnFirstLogin(dto);
        return R.ok();
    }
}
