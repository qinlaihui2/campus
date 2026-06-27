package com.campus.announcement.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.campus.announcement.entity.Announcement;
import com.campus.announcement.entity.AnnouncementAttachment;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface AnnouncementService extends IService<Announcement> {

    // 公开接口
    Page<Announcement> listByCategory(String category, int page, int size);

    List<Announcement> listCarousel();

    Announcement getDetail(Long id);

    // Admin 接口
    void createAnnouncement(Announcement announcement, Long publisherId);

    void updateAnnouncement(Long id, Announcement announcement);

    void deleteAnnouncement(Long id);

    // 附件
    void addAttachment(Long announcementId, MultipartFile file);

    List<AnnouncementAttachment> getAttachments(Long announcementId);

    void deleteAttachment(Long attachmentId);

    AnnouncementAttachment getAttachmentById(Long attachmentId);
}
