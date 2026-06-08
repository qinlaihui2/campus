package com.campus.feedback.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.feedback.entity.Feedback;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

@Mapper
public interface FeedbackMapper extends BaseMapper<Feedback> {

    @Select("SELECT feedback_type, COUNT(*) as count FROM feedback WHERE message_id = #{messageId} GROUP BY feedback_type")
    java.util.List<Map<String, Object>> countByType(Long messageId);
}
