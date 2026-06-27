package com.campus.square.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.square.dto.CommentVO;
import com.campus.square.dto.PublishRequest;
import com.campus.square.dto.SquarePostVO;

import java.util.List;

public interface SquarePostService {

    Page<SquarePostVO> listPosts(String category, String keyword, int page, int size, Long currentUserId);

    Page<SquarePostVO> listHot(int page, int size, Long currentUserId);

    SquarePostVO getDetail(Long id, Long currentUserId);

    void publish(PublishRequest request, Long userId);

    boolean toggleLike(Long postId, Long userId);

    Page<SquarePostVO> listMyPosts(int page, int size, Long userId);

    boolean toggleFavorite(Long postId, Long userId);

    void deletePost(Long id);

    // 评论
    List<com.campus.square.dto.CommentVO> getComments(Long postId, String sort, Long currentUserId);

    com.campus.square.dto.CommentVO addComment(Long postId, Long userId, String content, Long parentId, Long replyToUserId);

    boolean toggleCommentLike(Long commentId, Long userId);

    void deleteComment(Long commentId, Long userId);
}
