package com.campus.user.service;

import com.campus.user.dto.LoginRequest;
import com.campus.user.dto.LoginResponse;
import com.campus.user.dto.RegisterRequest;
import com.campus.common.entity.User;

public interface UserService {

    LoginResponse login(LoginRequest request);

    void register(RegisterRequest request);

    User getById(Long id);

    boolean updateById(User user);

    void sendVerifyCode(String email);
}
