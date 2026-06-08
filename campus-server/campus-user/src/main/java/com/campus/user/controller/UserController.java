package com.campus.user.controller;

import com.campus.common.result.R;
import com.campus.common.utils.UserContext;
import com.campus.common.entity.User;
import com.campus.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    public R<User> profile() {
        User user = userService.getById(UserContext.getUserId());
        user.setPassword(null);
        return R.ok(user);
    }

    @PutMapping("/profile")
    public R<String> updateProfile(@RequestBody User user) {
        user.setId(UserContext.getUserId());
        user.setPassword(null);
        user.setRole(null);
        user.setStatus(null);
        userService.updateById(user);
        return R.ok("更新成功");
    }
}
