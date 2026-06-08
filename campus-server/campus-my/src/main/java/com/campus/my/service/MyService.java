package com.campus.my.service;

import com.campus.my.dto.MyItemVO;

import java.util.List;

public interface MyService {

    /** 查询用户点赞列表（聚合课程+广场帖子），按时间倒序 */
    List<MyItemVO> listLikes(Long userId, int page, int size);

    /** 查询用户收藏列表（课程收藏），按时间倒序 */
    List<MyItemVO> listFavorites(Long userId, int page, int size);
}
