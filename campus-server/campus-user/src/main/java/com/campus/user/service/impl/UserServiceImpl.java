package com.campus.user.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.common.exception.BusinessException;
import com.campus.common.result.ResultCode;
import com.campus.security.service.JwtService;
import com.campus.user.dto.LoginRequest;
import com.campus.user.dto.LoginResponse;
import com.campus.user.dto.RegisterRequest;
import com.campus.common.entity.User;
import com.campus.common.mapper.UserMapper;
import com.campus.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final StringRedisTemplate redisTemplate;

    private static final String LOGIN_LOCK_KEY = "login:lock:";
    private static final String VERIFY_CODE_KEY = "verify:code:";
    private static final int MAX_LOGIN_FAILS = 5;
    private static final int LOCK_DURATION_MINUTES = 15;

    @Override
    public LoginResponse login(LoginRequest request) {
        String lockKey = LOGIN_LOCK_KEY + request.getUsername();
        String failCountStr = redisTemplate.opsForValue().get(lockKey);
        if (failCountStr != null && Integer.parseInt(failCountStr) >= MAX_LOGIN_FAILS) {
            throw new BusinessException(ResultCode.ACCOUNT_LOCKED);
        }

        User user = this.getOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername()));
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        if (user.getStatus() == 0) {
            throw new BusinessException(ResultCode.ACCOUNT_LOCKED);
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            recordLoginFail(request.getUsername());
            throw new BusinessException(ResultCode.PASSWORD_ERROR);
        }

        redisTemplate.delete(lockKey);

        String accessToken = jwtService.generateAccessToken(user.getId(), user.getUsername(), user.getRole());
        String refreshToken = jwtService.generateRefreshToken(user.getId(), user.getUsername(), user.getRole());

        user.setLastLoginTime(LocalDateTime.now());
        this.updateById(user);

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .role(user.getRole())
                .build();
    }

    @Override
    public void register(RegisterRequest request) {
        long count = this.count(
                new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername()));
        if (count > 0) {
            throw new BusinessException(ResultCode.USERNAME_EXISTS);
        }

        String role = request.getRole() != null ? request.getRole() : "STUDENT";
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setRole(role);
        user.setStatus(1);
        this.save(user);
    }

    @Override
    public boolean updateById(User user) {
        return super.updateById(user);
    }

    @Override
    public User getById(Long id) {
        User user = super.getById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        return user;
    }

    @Override
    public void sendVerifyCode(String email) {
        String code = RandomUtil.randomNumbers(6);
        redisTemplate.opsForValue().set(VERIFY_CODE_KEY + email, code, 5, TimeUnit.MINUTES);
        log.info("验证码发送到 {}: {}", email, code);
    }

    private void recordLoginFail(String username) {
        String lockKey = LOGIN_LOCK_KEY + username;
        redisTemplate.opsForValue().increment(lockKey);
        redisTemplate.expire(lockKey, LOCK_DURATION_MINUTES, TimeUnit.MINUTES);
    }
}
