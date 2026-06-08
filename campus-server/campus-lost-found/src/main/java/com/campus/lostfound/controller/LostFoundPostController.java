package com.campus.lostfound.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.common.result.PageResult;
import com.campus.common.result.R;
import com.campus.lostfound.dto.LostFoundPostVO;
import com.campus.lostfound.entity.LostFoundPost;
import com.campus.lostfound.service.LostFoundPostService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/lost-found")
@RequiredArgsConstructor
public class LostFoundPostController {

    private final LostFoundPostService lostFoundPostService;

    @GetMapping
    public R<PageResult<LostFoundPostVO>> list(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size) {
        Page<LostFoundPostVO> result = lostFoundPostService.listPosts(type, category, keyword, page, size);
        return R.ok(PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords()));
    }

    @GetMapping("/{id}")
    public R<LostFoundPostVO> detail(@PathVariable Long id) {
        return R.ok(lostFoundPostService.getDetail(id));
    }
}
