package com.zorvyn.finance.service.impl;

import com.zorvyn.finance.cache.CacheNames;
import com.zorvyn.finance.dto.response.CategoryTotalResponse;
import com.zorvyn.finance.dto.response.DashboardSummaryResponse;
import com.zorvyn.finance.dto.response.FinancialRecordResponse;
import com.zorvyn.finance.dto.response.MonthlyTrendResponse;
import com.zorvyn.finance.entity.RecordType;
import com.zorvyn.finance.mapper.FinancialRecordMapper;
import com.zorvyn.finance.repository.FinancialRecordRepository;
import com.zorvyn.finance.service.DashboardService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private static final Logger LOGGER = LoggerFactory.getLogger(
        DashboardServiceImpl.class
    );

    private final FinancialRecordRepository financialRecordRepository;

    public DashboardServiceImpl(
        FinancialRecordRepository financialRecordRepository
    ) {
        this.financialRecordRepository = financialRecordRepository;
    }

    @Override
    @Cacheable(
        cacheNames = CacheNames.DASHBOARD_SUMMARY,
        key = "T(java.lang.String).format('%s|%s|%d', #dateFrom, #dateTo, #recentLimit)"
    )
    public DashboardSummaryResponse getSummary(
        LocalDate dateFrom,
        LocalDate dateTo,
        int recentLimit
    ) {
        LOGGER.info(
            "Dashboard summary cache MISS. Computing from DB for dateFrom={}, dateTo={}, recentLimit={}",
            dateFrom,
            dateTo,
            recentLimit
        );

        BigDecimal totalIncome = nullSafe(
            financialRecordRepository.sumAmountByTypeInDateRange(
                RecordType.INCOME,
                dateFrom,
                dateTo
            )
        );
        BigDecimal totalExpense = nullSafe(
            financialRecordRepository.sumAmountByTypeInDateRange(
                RecordType.EXPENSE,
                dateFrom,
                dateTo
            )
        );
        BigDecimal netBalance = totalIncome.subtract(totalExpense);

        List<CategoryTotalResponse> categoryTotals =
            financialRecordRepository.findCategoryTotals(dateFrom, dateTo);

        List<FinancialRecordResponse> recentActivity = financialRecordRepository
            .findRecentActivity(
                dateFrom,
                dateTo,
                PageRequest.of(
                    0,
                    recentLimit,
                    Sort.by(Sort.Direction.DESC, "createdAt")
                )
            )
            .map(FinancialRecordMapper::toResponse)
            .getContent();

        List<MonthlyTrendResponse> monthlyTrends = financialRecordRepository
            .findMonthlyTrends(dateFrom, dateTo)
            .stream()
            .map(this::mapMonthlyTrend)
            .toList();

        return new DashboardSummaryResponse(
            totalIncome,
            totalExpense,
            netBalance,
            categoryTotals,
            recentActivity,
            monthlyTrends
        );
    }

    private MonthlyTrendResponse mapMonthlyTrend(Object[] row) {
        String month = row[0] == null ? null : row[0].toString();
        BigDecimal income = numberToBigDecimal(row[1]);
        BigDecimal expense = numberToBigDecimal(row[2]);
        BigDecimal net = income.subtract(expense);

        return new MonthlyTrendResponse(month, income, expense, net);
    }

    private BigDecimal numberToBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal number) {
            return number;
        }
        return new BigDecimal(value.toString());
    }

    private BigDecimal nullSafe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
