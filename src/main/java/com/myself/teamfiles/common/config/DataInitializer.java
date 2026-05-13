package com.myself.teamfiles.common.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.myself.teamfiles.module.user.entity.User;
import com.myself.teamfiles.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        initAdminUser();
    }

    private void initAdminUser() {
        User admin = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, "admin")
        );
        if (admin == null) {
            admin = new User();
            admin.setUsername("admin");
            admin.setRole("ADMIN");
            admin.setStatus(1);
            admin.setFirstLogin(1);
            admin.setNickname("System Admin");
            admin.setRemark("Built-in administrator account");
            admin.setStorageQuota(5L * 1024 * 1024 * 1024);
        }
        // Always reset password to ensure correct BCrypt hash
        admin.setPassword(passwordEncoder.encode("admin123"));
        if (admin.getId() == null) {
            userMapper.insert(admin);
        } else {
            userMapper.updateById(admin);
        }
        log.info("Default admin user ready: admin/admin123");
    }
}
