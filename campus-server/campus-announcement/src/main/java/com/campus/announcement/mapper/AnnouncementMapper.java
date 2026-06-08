package com.campus.announcement.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.announcement.entity.Announcement;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AnnouncementMapper extends BaseMapper<Announcement> {
}
