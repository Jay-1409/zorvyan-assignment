package com.zorvyn.finance.integration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zorvyn.finance.dto.request.CreateFinancialRecordRequest;
import com.zorvyn.finance.entity.FinancialRecord;
import com.zorvyn.finance.entity.RecordType;
import com.zorvyn.finance.repository.FinancialRecordRepository;
import com.zorvyn.finance.service.DashboardService;
import com.zorvyn.finance.service.FinancialRecordService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class DashboardCacheIntegrationTest {

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private FinancialRecordService financialRecordService;

    @MockBean
    private FinancialRecordRepository financialRecordRepository;

    @BeforeEach
    void setUp() {
        when(financialRecordRepository.sumAmountByTypeInDateRange(any(), any(), any()))
            .thenAnswer(invocation -> {
                RecordType type = invocation.getArgument(0);
                return type == RecordType.INCOME
                    ? new BigDecimal("1000.00")
                    : new BigDecimal("250.00");
            });
        when(financialRecordRepository.findCategoryTotals(any(), any()))
            .thenReturn(List.of());
        when(financialRecordRepository.findRecentActivity(any(), any(), any()))
            .thenReturn(Page.empty());
        when(financialRecordRepository.findMonthlyTrends(any(), any()))
            .thenReturn(List.of());
        when(financialRecordRepository.save(any(FinancialRecord.class)))
            .thenAnswer(invocation -> {
                FinancialRecord record = invocation.getArgument(0);
                record.setId(999L);
                return record;
            });
    }

    @Test
    void dashboardSummary_shouldUseCacheThenRecomputeAfterWriteEviction() {
        LocalDate dateFrom = LocalDate.of(2026, 1, 1);
        LocalDate dateTo = LocalDate.of(2026, 12, 31);
        int recentLimit = 5;

        dashboardService.getSummary(dateFrom, dateTo, recentLimit);
        dashboardService.getSummary(dateFrom, dateTo, recentLimit);

        verify(financialRecordRepository, times(1))
            .sumAmountByTypeInDateRange(
                eq(RecordType.INCOME),
                eq(dateFrom),
                eq(dateTo)
            );
        verify(financialRecordRepository, times(1))
            .sumAmountByTypeInDateRange(
                eq(RecordType.EXPENSE),
                eq(dateFrom),
                eq(dateTo)
            );
        verify(financialRecordRepository, times(1))
            .findCategoryTotals(eq(dateFrom), eq(dateTo));
        verify(financialRecordRepository, times(1))
            .findRecentActivity(eq(dateFrom), eq(dateTo), any());
        verify(financialRecordRepository, times(1))
            .findMonthlyTrends(eq(dateFrom), eq(dateTo));

        CreateFinancialRecordRequest request = new CreateFinancialRecordRequest(
            new BigDecimal("120.00"),
            RecordType.EXPENSE,
            "Food",
            "Lunch",
            LocalDate.of(2026, 4, 2)
        );
        financialRecordService.createRecord(request);

        dashboardService.getSummary(dateFrom, dateTo, recentLimit);

        verify(financialRecordRepository, times(2))
            .sumAmountByTypeInDateRange(
                eq(RecordType.INCOME),
                eq(dateFrom),
                eq(dateTo)
            );
        verify(financialRecordRepository, times(2))
            .sumAmountByTypeInDateRange(
                eq(RecordType.EXPENSE),
                eq(dateFrom),
                eq(dateTo)
            );
        verify(financialRecordRepository, times(2))
            .findCategoryTotals(eq(dateFrom), eq(dateTo));
        verify(financialRecordRepository, times(2))
            .findRecentActivity(eq(dateFrom), eq(dateTo), any());
        verify(financialRecordRepository, times(2))
            .findMonthlyTrends(eq(dateFrom), eq(dateTo));
    }
}
