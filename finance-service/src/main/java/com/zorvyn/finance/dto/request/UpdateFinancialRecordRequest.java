package com.zorvyn.finance.dto.request;

import com.zorvyn.finance.entity.RecordType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateFinancialRecordRequest(
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @Digits(integer = 15, fraction = 2, message = "Amount format is invalid")
    BigDecimal amount,

    RecordType type,

    @Size(min = 2, max = 50, message = "Category must be between 2 and 50 characters")
    String category,

    @Size(max = 500, message = "Description must be at most 500 characters")
    String description,

    LocalDate transactionDate
) {
}
