package com.campus.chat.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.chat.entity.Emoji;
import com.campus.chat.mapper.EmojiMapper;
import com.campus.common.result.R;
import com.campus.common.utils.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/emoji")
@RequiredArgsConstructor
public class EmojiController {

    private final EmojiMapper emojiMapper;
    private final JdbcTemplate jdbcTemplate;

    @GetMapping("/categories")
    public R<List<String>> categories() {
        List<String> categories = emojiMapper.selectList(null).stream()
                .map(Emoji::getCategory)
                .distinct()
                .toList();
        return R.ok(categories);
    }

    @GetMapping
    public R<List<Emoji>> list(@RequestParam(required = false) String category) {
        LambdaQueryWrapper<Emoji> wrapper = new LambdaQueryWrapper<>();
        if (category != null && !category.isEmpty()) {
            wrapper.eq(Emoji::getCategory, category);
        }
        wrapper.orderByAsc(Emoji::getSortOrder);
        return R.ok(emojiMapper.selectList(wrapper));
    }

    @GetMapping("/search")
    public R<List<Emoji>> search(@RequestParam String keyword) {
        LambdaQueryWrapper<Emoji> wrapper = new LambdaQueryWrapper<Emoji>()
                .and(w -> w.like(Emoji::getName, keyword)
                        .or().like(Emoji::getTags, keyword))
                .orderByAsc(Emoji::getSortOrder);
        return R.ok(emojiMapper.selectList(wrapper));
    }

    @GetMapping("/favorites")
    public R<List<Emoji>> favorites() {
        Long userId = UserContext.getUserId();
        List<Long> emojiIds = jdbcTemplate.queryForList(
                "SELECT emoji_id FROM emoji_favorite WHERE user_id = ?", Long.class, userId);
        if (emojiIds.isEmpty()) {
            return R.ok(List.of());
        }
        return R.ok(emojiMapper.selectBatchIds(emojiIds));
    }

    @PostMapping("/{id}/favorite")
    public R<Map<String, Boolean>> toggleFavorite(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        int count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM emoji_favorite WHERE user_id = ? AND emoji_id = ?",
                Integer.class, userId, id);
        if (count > 0) {
            jdbcTemplate.update("DELETE FROM emoji_favorite WHERE user_id = ? AND emoji_id = ?", userId, id);
            return R.ok(Map.of("favorited", false));
        } else {
            jdbcTemplate.update("INSERT INTO emoji_favorite (user_id, emoji_id) VALUES (?, ?)", userId, id);
            return R.ok(Map.of("favorited", true));
        }
    }
}
