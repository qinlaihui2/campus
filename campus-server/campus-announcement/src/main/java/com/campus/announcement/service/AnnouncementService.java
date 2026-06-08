package com.campus.announcement.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.campus.announcement.entity.Announcement;
import com.campus.announcement.entity.AnnouncementAttachment;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface AnnouncementService extends IService<Announcement> {

    Page<Announcement> listByCategory(String category, int page, int size);

    List<Announcement> listCarousel();

    Announcement getDetail(Long id);

    void addAttachment(Long announcementId, MultipartFile file);

    List<AnnouncementAttachment> getAttachments(Long announcementId);

    void deleteAttachment(Long attachmentId);

    AnnouncementAttachment getAttachmentById(Long attachmentId);
}
