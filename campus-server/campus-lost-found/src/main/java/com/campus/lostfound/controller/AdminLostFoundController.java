package com.campus.lostfound.controller;

import com.campus.common.result.R;
import com.campus.common.utils.UserContext;
import com.campus.lostfound.entity.LostFoundPost;
import com.campus.lostfound.service.LostFoundPostService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AdminLostFoundController {

    private final LostFoundPostService lostFoundPostService;

    @PostMapping("/lost-found")
    public R<Long> publish(@RequestBody LostFoundPost post) {
        post.setPublisherId(UserContext.getUserId());
        lostFoundPostService.publish(post);
        return R.ok(post.getId());
    }

    @PutMapping("/lost-found/{id}")
    public R<String> update(@PathVariable Long id, @RequestBody LostFoundPost post) {
        lostFoundPostService.updatePost(id, post, UserContext.getUserId());
        return R.ok("更新成功");
    }

    @PostMapping("/lost-found/{id}/resolve")
    public R<String> resolve(@PathVariable Long id) {
        lostFoundPostService.resolve(id, UserContext.getUserId());
        return R.ok("已标记完结");
    }

    @DeleteMapping("/admin/lost-found/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public R<String> delete(@PathVariable Long id) {
        lostFoundPostService.removeById(id);
        return R.ok("删除成功");
    }
}
