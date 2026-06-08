package com.campus.lostfound.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.campus.lostfound.dto.LostFoundPostVO;
import com.campus.lostfound.entity.LostFoundPost;

public interface LostFoundPostService extends IService<LostFoundPost> {

    Page<LostFoundPostVO> listPosts(String type, String category, String keyword, int page, int size);

    LostFoundPostVO getDetail(Long id);

    void publish(LostFoundPost post);

    void updatePost(Long id, LostFoundPost post, Long userId);

    void resolve(Long id, Long userId);
}
