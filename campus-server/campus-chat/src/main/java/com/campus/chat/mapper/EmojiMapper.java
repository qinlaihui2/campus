package com.campus.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.chat.entity.Emoji;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface EmojiMapper extends BaseMapper<Emoji> {
}
