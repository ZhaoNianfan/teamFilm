package com.myself.teamfiles.module.user.service;

import com.myself.teamfiles.common.result.PageResult;
import com.myself.teamfiles.module.user.dto.CreateUserDTO;
import com.myself.teamfiles.module.user.dto.UserPageDTO;
import com.myself.teamfiles.module.user.dto.UserVO;

public interface UserService {
    UserVO createUser(CreateUserDTO dto, Long createdBy);
    PageResult<UserVO> getUserList(UserPageDTO dto);
    UserVO getUserById(Long id);
    void updateUserStatus(Long id, Integer status);
    void updateUserRole(Long id, String role);
    void updateUser(Long id, String nickname, String role, Integer status, Long storageQuota);
    void deleteUser(Long id, String fileHandle);
    void resetPassword(Long id, String newPassword);
}
