package com.campus.market.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class PublishItemRequest {

    @NotBlank(message = "标题不能为空")
    @Size(max = 200, message = "标题最多200字")
    private String title;

    @NotBlank(message = "描述不能为空")
    private String description;

    @NotNull(message = "价格不能为空")
    @Positive(message = "价格必须大于0")
    private BigDecimal price;

    private BigDecimal originalPrice;

    @NotBlank(message = "分类不能为空")
    private String category;

    @NotBlank(message = "成色不能为空")
    private String condition;

    /** 图片 URL 列表（先通过 /api/upload/image 上传后获取） */
    private List<String> images;
}
