package com.zorvyn.finance.service;

import com.zorvyn.finance.dto.request.CreateFinancialRecordRequest;
import com.zorvyn.finance.dto.request.UpdateFinancialRecordRequest;
import com.zorvyn.finance.dto.response.FinancialRecordResponse;
import com.zorvyn.common.dto.PagedResponse;
import com.zorvyn.finance.entity.RecordType;
import java.math.BigDecimal;
import java.time.LocalDate;

public interface FinancialRecordService {

    FinancialRecordResponse createRecord(CreateFinancialRecordRequest request);

    FinancialRecordResponse getRecordById(Long id);

    PagedResponse<FinancialRecordResponse> listRecords(
        RecordType type,
        String category,
        LocalDate dateFrom,
        LocalDate dateTo,
        BigDecimal minAmount,
        BigDecimal maxAmount,
        String search,
        int page,
        int size,
        String sortBy,
        String sortDir
    );

    FinancialRecordResponse updateRecord(Long id, UpdateFinancialRecordRequest request);

    void deleteRecord(Long id);
}
