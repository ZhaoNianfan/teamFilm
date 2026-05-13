package com.myself.teamfiles.scheduled;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.myself.teamfiles.module.user.entity.User;
import com.myself.teamfiles.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoginUnlockTask {

    private final UserMapper userMapper;

    @Scheduled(fixedRate = 60000) // Every 60 seconds
    public void unlockAccounts() {
        List<User> lockedUsers = userMapper.selectList(
                new LambdaQueryWrapper<User>()
                        .isNotNull(User::getLockedUntil)
                        .lt(User::getLockedUntil, LocalDateTime.now())
        );
        for (User user : lockedUsers) {
            user.setLockedUntil(null);
            user.setLoginFailCount(0);
            userMapper.updateById(user);
            log.info("Auto-unlocked account: {}", user.getUsername());
        }
    }
}
