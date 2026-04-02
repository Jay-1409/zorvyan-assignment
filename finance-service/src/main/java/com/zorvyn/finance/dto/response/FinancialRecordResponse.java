package com.zorvyn.finance.dto.response;

import com.zorvyn.finance.entity.RecordType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record FinancialRecordResponse(
    Long id,
    BigDecimal amount,
    RecordType type,
    String category,
    String description,
    LocalDate transactionDate,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
