package com.zorvyn.finance.service;

import com.zorvyn.finance.dto.response.DashboardSummaryResponse;
import java.time.LocalDate;

public interface DashboardService {

    DashboardSummaryResponse getSummary(LocalDate dateFrom, LocalDate dateTo, int recentLimit);
}
