package com.campus.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.message.entity.Conversation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ImConversationMapper extends BaseMapper<Conversation> {

    @Select("SELECT * FROM im_conversation WHERE (user1_id = #{userId} AND user1_deleted = 0) OR (user2_id = #{userId} AND user2_deleted = 0) ORDER BY last_message_at DESC")
    IPage<Conversation> selectByUser(Page<Conversation> page, @Param("userId") Long userId);
}
