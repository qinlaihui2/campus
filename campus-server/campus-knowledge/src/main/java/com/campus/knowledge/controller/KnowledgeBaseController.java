package com.campus.knowledge.controller;

import com.campus.common.result.R;
import com.campus.knowledge.entity.KnowledgeBase;
import com.campus.knowledge.service.KnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/knowledge-base")
@RequiredArgsConstructor
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;

    @GetMapping("/list")
    public R<List<KnowledgeBase>> list() {
        return R.ok(knowledgeBaseService.list());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public R<String> create(@RequestBody KnowledgeBase knowledgeBase) {
        knowledgeBaseService.save(knowledgeBase);
        return R.ok("创建成功");
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public R<String> update(@RequestBody KnowledgeBase knowledgeBase) {
        knowledgeBaseService.updateById(knowledgeBase);
        return R.ok("更新成功");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public R<String> delete(@PathVariable Long id) {
        knowledgeBaseService.removeById(id);
        return R.ok("删除成功");
    }
}
