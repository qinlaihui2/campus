package com.campus.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.message.entity.Message;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ImMessageMapper extends BaseMapper<Message> {

    @Select("SELECT COUNT(*) FROM im_message WHERE conversation_id IN (SELECT id FROM im_conversation WHERE (user1_id = #{userId} AND user1_deleted = 0) OR (user2_id = #{userId} AND user2_deleted = 0)) AND sender_id != #{userId} AND is_read = 0")
    int countUnread(@Param("userId") Long userId);
}
