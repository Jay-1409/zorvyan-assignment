package com.zorvyn.finance.mapper;

import com.zorvyn.finance.dto.response.FinancialRecordResponse;
import com.zorvyn.finance.entity.FinancialRecord;

public final class FinancialRecordMapper {

    private FinancialRecordMapper() {
    }

    public static FinancialRecordResponse toResponse(FinancialRecord record) {
        return new FinancialRecordResponse(
            record.getId(),
            record.getAmount(),
            record.getType(),
            record.getCategory(),
            record.getDescription(),
            record.getTransactionDate(),
            record.getCreatedAt(),
            record.getUpdatedAt()
        );
    }
}
