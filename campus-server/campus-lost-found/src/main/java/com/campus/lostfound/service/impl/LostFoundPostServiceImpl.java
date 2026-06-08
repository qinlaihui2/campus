package com.campus.lostfound.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.common.entity.User;
import com.campus.common.exception.BusinessException;
import com.campus.common.mapper.UserMapper;
import com.campus.common.result.ResultCode;
import com.campus.lostfound.dto.LostFoundPostVO;
import com.campus.lostfound.entity.LostFoundPost;
import com.campus.lostfound.mapper.LostFoundPostMapper;
import com.campus.lostfound.service.LostFoundPostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class LostFoundPostServiceImpl extends ServiceImpl<LostFoundPostMapper, LostFoundPost> implements LostFoundPostService {

    private final UserMapper userMapper;

    @Override
    public Page<LostFoundPostVO> listPosts(String type, String category, String keyword, int page, int size) {
        Page<LostFoundPost> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<LostFoundPost> wrapper = new LambdaQueryWrapper<LostFoundPost>()
                .eq(LostFoundPost::getStatus, "OPEN")
                .orderByDesc(LostFoundPost::getPublishedAt);

        if (StringUtils.hasText(type) && !"ALL".equalsIgnoreCase(type)) {
            wrapper.eq(LostFoundPost::getType, type.toUpperCase());
        }
        if (StringUtils.hasText(category)) {
            wrapper.eq(LostFoundPost::getCategory, category);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(LostFoundPost::getTitle, keyword).or().like(LostFoundPost::getDescription, keyword));
        }

        Page<LostFoundPost> result = this.page(pageParam, wrapper);

        // to VO with publisher info
        Page<LostFoundPostVO> voPage = new Page<>(page, size, result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(p -> {
            LostFoundPostVO vo = new LostFoundPostVO();
            org.springframework.beans.BeanUtils.copyProperties(p, vo);
            User u = userMapper.selectById(p.getPublisherId());
            if (u != null) {
                vo.setPublisherName(u.getNickname() != null ? u.getNickname() : u.getUsername());
                vo.setPublisherAvatar(u.getAvatar());
            }
            return vo;
        }).toList());
        return voPage;
    }

    @Override
    public LostFoundPostVO getDetail(Long id) {
        LostFoundPost p = this.getById(id);
        if (p == null) throw new BusinessException(ResultCode.NOT_FOUND);
        LostFoundPostVO vo = new LostFoundPostVO();
        org.springframework.beans.BeanUtils.copyProperties(p, vo);
        User u = userMapper.selectById(p.getPublisherId());
        if (u != null) {
            vo.setPublisherName(u.getNickname() != null ? u.getNickname() : u.getUsername());
            vo.setPublisherAvatar(u.getAvatar());
        }
        return vo;
    }

    @Override
    @Transactional
    public void publish(LostFoundPost post) {
        post.setStatus("OPEN");
        post.setPublishedAt(LocalDateTime.now());
        this.save(post);
    }

    @Override
    @Transactional
    public void updatePost(Long id, LostFoundPost post, Long userId) {
        LostFoundPost existing = this.getById(id);
        if (existing == null) throw new BusinessException(ResultCode.NOT_FOUND);
        if (!existing.getPublisherId().equals(userId)) throw new BusinessException(ResultCode.FORBIDDEN);
        post.setId(id);
        this.updateById(post);
    }

    @Override
    @Transactional
    public void resolve(Long id, Long userId) {
        LostFoundPost existing = this.getById(id);
        if (existing == null) throw new BusinessException(ResultCode.NOT_FOUND);
        if (!existing.getPublisherId().equals(userId)) throw new BusinessException(ResultCode.FORBIDDEN);
        existing.setStatus("RESOLVED");
        this.updateById(existing);
    }
}
