package com.campus.announcement.controller;

import com.campus.announcement.entity.Announcement;
import com.campus.announcement.entity.AnnouncementAttachment;
import com.campus.announcement.service.AnnouncementService;
import com.campus.common.result.R;
import com.campus.common.utils.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin/announcements")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminAnnouncementController {

    private final AnnouncementService announcementService;

    @PostMapping
    @CacheEvict(value = {"announcement:list", "announcement:carousel"}, allEntries = true)
    public R<Announcement> create(@RequestBody Announcement announcement) {
        announcement.setPublisherId(UserContext.getUserId());
        announcement.setPublishedAt(LocalDateTime.now());
        announcement.setStatus("PUBLISHED");
        announcementService.save(announcement);
        return R.ok(announcement);
    }

    @PutMapping("/{id}")
    @CacheEvict(value = {"announcement:list", "announcement:carousel"}, allEntries = true)
    public R<String> update(@PathVariable Long id, @RequestBody Announcement announcement) {
        announcement.setId(id);
        announcementService.updateById(announcement);
        return R.ok("更新成功");
    }

    @DeleteMapping("/{id}")
    @CacheEvict(value = {"announcement:list", "announcement:carousel"}, allEntries = true)
    public R<String> delete(@PathVariable Long id) {
        announcementService.removeById(id);
        return R.ok("删除成功");
    }

    @PostMapping("/{id}/carousel")
    @CacheEvict(value = {"announcement:list", "announcement:carousel"}, allEntries = true)
    public R<String> toggleCarousel(@PathVariable Long id, @RequestParam int isCarousel, @RequestParam(defaultValue = "0") int sort) {
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
