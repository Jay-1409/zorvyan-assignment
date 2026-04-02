package com.zorvyn.finance.dto.response;

import java.math.BigDecimal;

public record CategoryTotalResponse(
    String category,
    BigDecimal total
) {
}
