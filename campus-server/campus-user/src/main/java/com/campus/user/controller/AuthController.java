package com.campus.user.controller;

import com.campus.common.result.R;
import com.campus.user.dto.LoginRequest;
import com.campus.user.dto.LoginResponse;
import com.campus.user.dto.RegisterRequest;
import com.campus.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/login")
    public R<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return R.ok("登录成功", userService.login(request));
    }

    @PostMapping("/register")
    public R<String> register(@Valid @RequestBody RegisterRequest request) {
        userService.register(request);
        return R.ok("注册成功");
    }

    @PostMapping("/verify-code")
    public R<String> sendVerifyCode(@RequestParam String email) {
        userService.sendVerifyCode(email);
        return R.ok("验证码已发送");
    }
}
