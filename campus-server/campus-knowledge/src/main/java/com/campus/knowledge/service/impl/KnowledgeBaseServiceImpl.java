package com.campus.knowledge.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.knowledge.entity.KnowledgeBase;
import com.campus.knowledge.mapper.KnowledgeBaseMapper;
import com.campus.knowledge.service.KnowledgeBaseService;
import org.springframework.stereotype.Service;

@Service
public class KnowledgeBaseServiceImpl extends ServiceImpl<KnowledgeBaseMapper, KnowledgeBase>
        implements KnowledgeBaseService {
}
