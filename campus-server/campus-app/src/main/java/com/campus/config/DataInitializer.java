package com.campus.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.common.entity.User;
import com.campus.common.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 数据初始化 —— 首次启动时自动创建超级管理员账号
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "admin123";

    @Override
    public void run(String... args) {
        initAdmin();
    }

    private void initAdmin() {
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, ADMIN_USERNAME));
        if (count > 0) {
            log.info("管理员账号已存在，跳过初始化");
            return;
        }

        User admin = new User();
        admin.setUsername(ADMIN_USERNAME);
        admin.setPassword(passwordEncoder.encode(ADMIN_PASSWORD));
        admin.setNickname("超级管理员");
        admin.setRole("ADMIN");
        admin.setStatus(1);
        userMapper.insert(admin);

        log.info("========================================");
        log.info("  超级管理员账号已创建");
        log.info("  用户名: admin");
        log.info("  密码:   admin123");
        log.info("========================================");
    }
}
