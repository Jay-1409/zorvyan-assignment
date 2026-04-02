package com.zorvyn.finance.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record DashboardSummaryResponse(
    BigDecimal totalIncome,
    BigDecimal totalExpense,
    BigDecimal netBalance,
    List<CategoryTotalResponse> categoryTotals,
    List<FinancialRecordResponse> recentActivity,
    List<MonthlyTrendResponse> monthlyTrends
) {
}
