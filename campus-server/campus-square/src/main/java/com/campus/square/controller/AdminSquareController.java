package com.campus.square.controller;

import com.campus.common.result.R;
import com.campus.square.service.SquarePostService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/square")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminSquareController {

    private final SquarePostService squarePostService;

    @DeleteMapping("/{id}")
    public R<String> delete(@PathVariable Long id) {
        squarePostService.deletePost(id);
        return R.ok("删除成功");
    }
}
