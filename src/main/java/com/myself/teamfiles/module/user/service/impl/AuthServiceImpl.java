package com.myself.teamfiles.module.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.myself.teamfiles.common.constant.SystemConstants;
import com.myself.teamfiles.common.exception.BusinessException;
import com.myself.teamfiles.common.exception.ErrorCode;
import com.myself.teamfiles.common.util.JwtTokenUtil;
import com.myself.teamfiles.module.user.dto.ChangePasswordDTO;
import com.myself.teamfiles.module.user.dto.LoginDTO;
import com.myself.teamfiles.module.user.dto.LoginVO;
import com.myself.teamfiles.module.user.dto.UserVO;
import com.myself.teamfiles.module.user.entity.User;
import com.myself.teamfiles.module.user.mapper.UserMapper;
import com.myself.teamfiles.module.user.service.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static com.myself.teamfiles.common.constant.UserConstants.LOGIN_MAX_FAIL_COUNT;

@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenUtil jwtTokenUtil;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    public AuthServiceImpl(UserMapper userMapper, PasswordEncoder passwordEncoder,
                           JwtTokenUtil jwtTokenUtil) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenUtil = jwtTokenUtil;
    }

    @Override
    public LoginVO login(LoginDTO loginDTO, String ip) {
        String username = loginDTO.getUsername();

        // Check login lock (Redis-based, skip if Redis unavailable)
        if (redisTemplate != null) {
            String failKey = SystemConstants.LOGIN_FAIL_PREFIX + username;
            String failCount = redisTemplate.opsForValue().get(failKey);
            if (failCount != null && Integer.parseInt(failCount) >= LOGIN_MAX_FAIL_COUNT) {
                throw new BusinessException(ErrorCode.USER_LOCKED);
            }
        }

        // Query user
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username)
        );
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException(ErrorCode.USER_DISABLED);
        }

        // Check locked_until
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.USER_LOCKED);
        }

        // Verify password
        if (!passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            incrementLoginFail(username, user);
            throw new BusinessException(ErrorCode.PASSWORD_ERROR);
        }

        // Reset fail count on success
        if (redisTemplate != null) {
            String failKey = SystemConstants.LOGIN_FAIL_PREFIX + username;
            redisTemplate.delete(failKey);
        }
        if (user.getLoginFailCount() != null && user.getLoginFailCount() > 0) {
            user.setLoginFailCount(0);
            user.setLockedUntil(null);
        }

        // First login check
        LoginVO loginVO = new LoginVO();
        if (user.getFirstLogin() != null && user.getFirstLogin() == 1) {
            loginVO.setNeedChangePassword(true);
            return loginVO;
        }

        // Update last login
        user.setLastLoginTime(LocalDateTime.now());
        user.setLastLoginIp(ip);
        userMapper.updateById(user);

        // Generate token
        String token = jwtTokenUtil.generateToken(user.getId(), user.getUsername(),
                user.getRole(), loginDTO.getRememberMe());
        loginVO.setToken(token);
        loginVO.setExpiresIn(jwtTokenUtil.getExpirationFromToken(token) / 1000);

        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        loginVO.setUserInfo(userVO);

        return loginVO;
    }

    @Override
    public void logout(String token) {
        if (redisTemplate != null) {
            long expire = jwtTokenUtil.getExpirationFromToken(token);
            if (expire > 0) {
                String blacklistKey = SystemConstants.TOKEN_BLACKLIST_PREFIX + token;
                redisTemplate.opsForValue().set(blacklistKey, "1", Duration.ofMillis(expire));
            }
        }
    }

    @Override
    public String refreshToken(String token) {
        if (!jwtTokenUtil.validateToken(token)) {
            throw new BusinessException(ErrorCode.TOKEN_EXPIRED);
        }
        if (redisTemplate != null) {
            String blacklistKey = SystemConstants.TOKEN_BLACKLIST_PREFIX + token;
            if (Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey))) {
                throw new BusinessException(ErrorCode.UNAUTHORIZED);
            }
            long expire = jwtTokenUtil.getExpirationFromToken(token);
            if (expire > 0) {
                redisTemplate.opsForValue().set(blacklistKey, "1", Duration.ofMillis(expire));
            }
        }

        Long userId = jwtTokenUtil.getUserIdFromToken(token);
        String username = jwtTokenUtil.getUsernameFromToken(token);
        String role = jwtTokenUtil.getRoleFromToken(token);

        return jwtTokenUtil.generateToken(userId, username, role, false);
    }

    @Override
    public void changePasswordOnFirstLogin(ChangePasswordDTO dto) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername())
        );
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        // Verify old password
        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_ERROR);
        }
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        user.setFirstLogin(0);
        userMapper.updateById(user);
    }

    @Override
    public UserVO getCurrentUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return userVO;
    }

    private void incrementLoginFail(String username, User user) {
        if (redisTemplate != null) {
            String failKey = SystemConstants.LOGIN_FAIL_PREFIX + username;
            Long count = redisTemplate.opsForValue().increment(failKey);
            redisTemplate.expire(failKey, 10, TimeUnit.MINUTES);

            if (count != null && count >= LOGIN_MAX_FAIL_COUNT) {
                user.setLockedUntil(LocalDateTime.now().plusMinutes(10));
                userMapper.updateById(user);
            }
        }
    }
}
