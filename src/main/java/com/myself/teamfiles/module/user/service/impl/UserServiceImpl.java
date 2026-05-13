package com.myself.teamfiles.module.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.myself.teamfiles.common.exception.BusinessException;
import com.myself.teamfiles.common.exception.ErrorCode;
import com.myself.teamfiles.common.result.PageResult;
import com.myself.teamfiles.module.user.dto.CreateUserDTO;
import com.myself.teamfiles.module.user.dto.UserPageDTO;
import com.myself.teamfiles.module.user.dto.UserVO;
import com.myself.teamfiles.module.user.entity.User;
import com.myself.teamfiles.module.user.mapper.UserMapper;
import com.myself.teamfiles.module.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserVO createUser(CreateUserDTO dto, Long createdBy) {
        // Check unique username
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername())
        );
        if (count > 0) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS);
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRole(dto.getRole());
        user.setNickname(StringUtils.hasText(dto.getNickname()) ? dto.getNickname() : dto.getUsername());
        user.setRemark(dto.getRemark());
        user.setCreatedBy(createdBy);
        user.setStorageQuota(5L * 1024 * 1024 * 1024); // 5GB default
        userMapper.insert(user);

        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        return vo;
    }

    @Override
    public PageResult<UserVO> getUserList(UserPageDTO dto) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(dto.getKeyword())) {
            wrapper.and(w -> w
                    .like(User::getUsername, dto.getKeyword())
                    .or()
                    .like(User::getNickname, dto.getKeyword())
            );
        }
        if (StringUtils.hasText(dto.getRole())) {
            wrapper.eq(User::getRole, dto.getRole());
        }
        if (dto.getStatus() != null) {
            wrapper.eq(User::getStatus, dto.getStatus());
        }
        wrapper.orderByDesc(User::getCreatedAt);

        Page<User> page = new Page<>(dto.getPage(), dto.getPageSize());
        Page<User> result = userMapper.selectPage(page, wrapper);

        List<UserVO> records = result.getRecords().stream().map(user -> {
            UserVO vo = new UserVO();
            BeanUtils.copyProperties(user, vo);
            return vo;
        }).toList();

        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), records);
    }

    @Override
    public UserVO getUserById(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        return vo;
    }

    @Override
    public void updateUserStatus(Long id, Integer status) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        user.setStatus(status);
        userMapper.updateById(user);
    }

    @Override
    public void updateUserRole(Long id, String role) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        user.setRole(role);
        userMapper.updateById(user);
    }

    @Override
    public void updateUser(Long id, String nickname, String role, Integer status, Long storageQuota) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        if (nickname != null) user.setNickname(nickname);
        if (role != null) user.setRole(role);
        if (status != null) user.setStatus(status);
        if (storageQuota != null && storageQuota >= 0) user.setStorageQuota(storageQuota);
        userMapper.updateById(user);
    }

    @Override
    public void deleteUser(Long id, String fileHandle) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        // TODO: Handle file transfer or deletion based on fileHandle param
        userMapper.deleteById(id);
    }

    @Override
    public void resetPassword(Long id, String newPassword) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setFirstLogin(1);
        userMapper.updateById(user);
    }
}
