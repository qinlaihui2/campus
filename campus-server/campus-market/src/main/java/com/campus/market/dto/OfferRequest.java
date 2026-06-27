package com.campus.market.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class OfferRequest {

    @NotNull(message = "出价不能为空")
    @Positive(message = "出价必须大于0")
    private BigDecimal price;

    private String message;
}
