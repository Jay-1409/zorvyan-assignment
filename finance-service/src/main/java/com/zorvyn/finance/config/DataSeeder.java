package com.zorvyn.finance.config;

import com.zorvyn.finance.entity.FinancialRecord;
import com.zorvyn.finance.entity.RecordType;
import com.zorvyn.finance.repository.FinancialRecordRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("!test")
public class DataSeeder implements CommandLineRunner {

    private final FinancialRecordRepository financialRecordRepository;

    public DataSeeder(FinancialRecordRepository financialRecordRepository) {
        this.financialRecordRepository = financialRecordRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (financialRecordRepository.count() > 0) {
            return;
        }

        LocalDate now = LocalDate.now();

        List<FinancialRecord> seedRecords = List.of(
            buildRecord(new BigDecimal("7000.00"), RecordType.INCOME, "Salary", "Monthly salary", now.minusDays(60)),
            buildRecord(new BigDecimal("320.00"), RecordType.EXPENSE, "Food", "Groceries", now.minusDays(55)),
            buildRecord(new BigDecimal("150.00"), RecordType.EXPENSE, "Transport", "Fuel and metro", now.minusDays(50)),
            buildRecord(new BigDecimal("500.00"), RecordType.INCOME, "Freelance", "Side project", now.minusDays(40)),
            buildRecord(new BigDecimal("240.00"), RecordType.EXPENSE, "Utilities", "Electricity + internet", now.minusDays(35)),
            buildRecord(new BigDecimal("7200.00"), RecordType.INCOME, "Salary", "Monthly salary", now.minusDays(30)),
            buildRecord(new BigDecimal("1300.00"), RecordType.EXPENSE, "Rent", "Apartment rent", now.minusDays(28)),
            buildRecord(new BigDecimal("410.00"), RecordType.EXPENSE, "Food", "Groceries + dining", now.minusDays(20)),
            buildRecord(new BigDecimal("900.00"), RecordType.INCOME, "Bonus", "Performance bonus", now.minusDays(10)),
            buildRecord(new BigDecimal("280.00"), RecordType.EXPENSE, "Health", "Medical checkup", now.minusDays(5))
        );

        financialRecordRepository.saveAll(seedRecords);
    }

    private FinancialRecord buildRecord(
        BigDecimal amount,
        RecordType type,
        String category,
        String description,
        LocalDate transactionDate
    ) {
        FinancialRecord record = new FinancialRecord();
        record.setAmount(amount);
        record.setType(type);
        record.setCategory(category);
        record.setDescription(description);
        record.setTransactionDate(transactionDate);
        record.setDeleted(false);
        return record;
    }
}
