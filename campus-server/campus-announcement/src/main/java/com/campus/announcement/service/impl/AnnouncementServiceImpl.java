package com.campus.announcement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.announcement.entity.Announcement;
import com.campus.announcement.entity.AnnouncementAttachment;
import com.campus.announcement.mapper.AnnouncementAttachmentMapper;
import com.campus.announcement.mapper.AnnouncementMapper;
import com.campus.announcement.service.AnnouncementService;
import com.campus.common.exception.BusinessException;
import com.campus.common.result.ResultCode;
import com.campus.file.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnnouncementServiceImpl extends ServiceImpl<AnnouncementMapper, Announcement> implements AnnouncementService {

    private final AnnouncementAttachmentMapper attachmentMapper;
    private final FileService fileService;

    // ==================== 公开接口 ====================

    @Override
    public Page<Announcement> listByCategory(String category, int page, int size) {
        Page<Announcement> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Announcement> wrapper = new LambdaQueryWrapper<Announcement>()
                .eq(Announcement::getStatus, "PUBLISHED")
                .orderByDesc(Announcement::getPriority)
                .orderByDesc(Announcement::getPublishedAt);
        if (category != null && !category.isEmpty() && !"ALL".equalsIgnoreCase(category)) {
            wrapper.eq(Announcement::getCategory, category);
        }
        return this.page(pageParam, wrapper);
    }

    @Override
    public List<Announcement> listCarousel() {
        LambdaQueryWrapper<Announcement> wrapper = new LambdaQueryWrapper<Announcement>()
                .eq(Announcement::getIsCarousel, 1)
                .eq(Announcement::getStatus, "PUBLISHED")
                .orderByDesc(Announcement::getCarouselSort)
                .orderByDesc(Announcement::getPublishedAt)
                .last("LIMIT 10");
        return this.list(wrapper);
    }

    @Override
    public Announcement getDetail(Long id) {
        Announcement announcement = this.getById(id);
        if (announcement == null || !"PUBLISHED".equals(announcement.getStatus())) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        return announcement;
    }

    // ==================== Admin 接口 ====================

    @Override
    @Transactional
    public void createAnnouncement(Announcement announcement, Long publisherId) {
        announcement.setPublisherId(publisherId);
        announcement.setPublishedAt(LocalDateTime.now());
        announcement.setStatus("PUBLISHED");
        announcement.setIsCarousel(announcement.getIsCarousel() != null ? announcement.getIsCarousel() : 0);
        announcement.setCarouselSort(announcement.getCarouselSort() != null ? announcement.getCarouselSort() : 0);
        announcement.setPriority(announcement.getPriority() != null ? announcement.getPriority() : 0);
        this.save(announcement);
    }

    @Override
    public void updateAnnouncement(Long id, Announcement announcement) {
        announcement.setId(id);
        this.updateById(announcement);
    }

    @Override
    public void deleteAnnouncement(Long id) {
        Announcement announcement = this.getById(id);
        if (announcement != null) {
            announcement.setStatus("ARCHIVED");
            this.updateById(announcement);
        }
    }

    // ==================== 附件 ====================

    @Override
    @Transactional
    public void addAttachment(Long announcementId, MultipartFile file) {
        String objectName = fileService.upload(file);
        AnnouncementAttachment attachment = new AnnouncementAttachment();
        attachment.setAnnouncementId(announcementId);
        attachment.setFileName(file.getOriginalFilename());
        attachment.setFileSize(file.getSize());
        attachment.setFileUrl(objectName);
        attachment.setCreatedAt(LocalDateTime.now());
        attachmentMapper.insert(attachment);
    }

    @Override
    public List<AnnouncementAttachment> getAttachments(Long announcementId) {
        LambdaQueryWrapper<AnnouncementAttachment> wrapper = new LambdaQueryWrapper<AnnouncementAttachment>()
                .eq(AnnouncementAttachment::getAnnouncementId, announcementId)
                .orderByAsc(AnnouncementAttachment::getCreatedAt);
        return attachmentMapper.selectList(wrapper);
    }

    @Override
    public void deleteAttachment(Long attachmentId) {
        attachmentMapper.deleteById(attachmentId);
    }

    @Override
    public AnnouncementAttachment getAttachmentById(Long attachmentId) {
        return attachmentMapper.selectById(attachmentId);
    }
}
