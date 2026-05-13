package com.myself.teamfiles.module.user.service;

import com.myself.teamfiles.module.user.dto.ChangePasswordDTO;
import com.myself.teamfiles.module.user.dto.LoginDTO;
import com.myself.teamfiles.module.user.dto.LoginVO;
import com.myself.teamfiles.module.user.dto.UserVO;

public interface AuthService {
    LoginVO login(LoginDTO loginDTO, String ip);
    void logout(String token);
    String refreshToken(String token);
    void changePasswordOnFirstLogin(ChangePasswordDTO dto);
    UserVO getCurrentUser(Long userId);
}
