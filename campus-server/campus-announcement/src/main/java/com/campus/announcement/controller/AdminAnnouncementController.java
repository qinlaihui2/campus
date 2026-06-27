package com.campus.announcement.controller;

import com.campus.announcement.entity.Announcement;
import com.campus.announcement.entity.AnnouncementAttachment;
import com.campus.announcement.service.AnnouncementService;
import com.campus.common.result.R;
import com.campus.common.utils.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/admin/announcements")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminAnnouncementController {

    private final AnnouncementService announcementService;

    @GetMapping
    public R<?> list(@RequestParam(defaultValue = "1") int page,
                     @RequestParam(defaultValue = "10") int size) {
        var result = announcementService.lambdaQuery()
                .ne(Announcement::getStatus, "ARCHIVED")
                .orderByDesc(Announcement::getPublishedAt)
                .page(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size));
        return R.ok(com.campus.common.result.PageResult.of(
                result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords()));
    }

    @PostMapping
    public R<String> create(@RequestBody Announcement announcement) {
        announcementService.createAnnouncement(announcement, UserContext.getUserId());
        return R.ok("创建成功");
    }

    @PutMapping("/{id}")
    public R<String> update(@PathVariable Long id, @RequestBody Announcement announcement) {
        announcementService.updateAnnouncement(id, announcement);
        return R.ok("更新成功");
    }

    @DeleteMapping("/{id}")
    public R<String> delete(@PathVariable Long id) {
        announcementService.deleteAnnouncement(id);
        return R.ok("删除成功");
    }

    @PostMapping("/{id}/carousel")
    public R<String> toggleCarousel(@PathVariable Long id, @RequestParam int isCarousel,
                                     @RequestParam(defaultValue = "0") int sort) {
        Announcement announcement = announcementService.getById(id);
        if (announcement == null) {
            return R.fail("公告不存在");
        }
        announcement.setIsCarousel(isCarousel);
        announcement.setCarouselSort(sort);
        announcementService.updateById(announcement);
        return R.ok(isCarousel == 1 ? "已加入轮播" : "已移出轮播");
    }

    @PostMapping("/{id}/attachment")
    public R<String> uploadAttachment(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        announcementService.addAttachment(id, file);
        return R.ok("附件上传成功");
    }

    @DeleteMapping("/{id}/attachment/{attachmentId}")
    public R<String> deleteAttachment(@PathVariable Long id, @PathVariable Long attachmentId) {
        List<AnnouncementAttachment> attachments = announcementService.getAttachments(id);
        boolean exists = attachments.stream().anyMatch(a -> a.getId().equals(attachmentId));
        if (!exists) {
            return R.fail("附件不属于该公告");
        }
        announcementService.deleteAttachment(attachmentId);
        return R.ok("附件删除成功");
    }
}
