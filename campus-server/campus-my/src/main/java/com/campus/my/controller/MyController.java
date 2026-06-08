package com.campus.my.controller;

import com.campus.common.result.R;
import com.campus.common.utils.UserContext;
import com.campus.my.dto.MyItemVO;
import com.campus.my.service.MyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/my")
@RequiredArgsConstructor
public class MyController {

    private final MyService myService;

    @GetMapping("/likes")
    public R<List<MyItemVO>> likes(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size) {
        Long userId = UserContext.getUserId();
        return R.ok(myService.listLikes(userId, page, size));
    }

    @GetMapping("/favorites")
    public R<List<MyItemVO>> favorites(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size) {
        Long userId = UserContext.getUserId();
        return R.ok(myService.listFavorites(userId, page, size));
    }
}
