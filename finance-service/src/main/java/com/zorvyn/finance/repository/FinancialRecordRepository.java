package com.zorvyn.finance.repository;

import com.zorvyn.finance.dto.response.CategoryTotalResponse;
import com.zorvyn.finance.entity.FinancialRecord;
import com.zorvyn.finance.entity.RecordType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FinancialRecordRepository
    extends JpaRepository<FinancialRecord, Long>, JpaSpecificationExecutor<FinancialRecord> {

    Optional<FinancialRecord> findByIdAndDeletedFalse(Long id);

    @Query("""
        select coalesce(sum(fr.amount), 0)
        from FinancialRecord fr
        where fr.deleted = false
          and fr.type = :type
          and (:dateFrom is null or fr.transactionDate >= :dateFrom)
          and (:dateTo is null or fr.transactionDate <= :dateTo)
        """)
    BigDecimal sumAmountByTypeInDateRange(
        @Param("type") RecordType type,
        @Param("dateFrom") LocalDate dateFrom,
        @Param("dateTo") LocalDate dateTo
    );

    @Query("""
        select new com.zorvyn.finance.dto.response.CategoryTotalResponse(fr.category, coalesce(sum(fr.amount), 0))
        from FinancialRecord fr
        where fr.deleted = false
          and (:dateFrom is null or fr.transactionDate >= :dateFrom)
          and (:dateTo is null or fr.transactionDate <= :dateTo)
        group by fr.category
        order by coalesce(sum(fr.amount), 0) desc
        """)
    List<CategoryTotalResponse> findCategoryTotals(
        @Param("dateFrom") LocalDate dateFrom,
        @Param("dateTo") LocalDate dateTo
    );

    @Query("""
        select fr
        from FinancialRecord fr
        where fr.deleted = false
          and (:dateFrom is null or fr.transactionDate >= :dateFrom)
          and (:dateTo is null or fr.transactionDate <= :dateTo)
        """)
    Page<FinancialRecord> findRecentActivity(
        @Param("dateFrom") LocalDate dateFrom,
        @Param("dateTo") LocalDate dateTo,
        Pageable pageable
    );

    @Query(value = """
        select to_char(transaction_date, 'YYYY-MM') as month,
               sum(case when type = 'INCOME' then amount else 0 end) as income_total,
               sum(case when type = 'EXPENSE' then amount else 0 end) as expense_total
        from financial_records
        where deleted = 0
          and (:dateFrom is null or transaction_date >= :dateFrom)
          and (:dateTo is null or transaction_date <= :dateTo)
        group by to_char(transaction_date, 'YYYY-MM')
        order by month
        """, nativeQuery = true)
    List<Object[]> findMonthlyTrends(
        @Param("dateFrom") LocalDate dateFrom,
        @Param("dateTo") LocalDate dateTo
    );
}
