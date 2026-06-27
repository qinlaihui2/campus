package com.campus.market.controller;

import com.campus.common.result.R;
import com.campus.market.service.MarketService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/market")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminMarketController {

    private final MarketService marketService;

    @DeleteMapping("/{id}")
    public R<String> delete(@PathVariable Long id) {
        // 管理员可以删除任意商品，userId 传 0 绕过权限检查
        marketService.deleteItem(id, 0L);
        return R.ok("删除成功");
    }
}
