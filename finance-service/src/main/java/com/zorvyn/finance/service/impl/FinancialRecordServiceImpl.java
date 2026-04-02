package com.zorvyn.finance.service.impl;

import com.zorvyn.finance.cache.CacheNames;
import com.zorvyn.finance.dto.request.CreateFinancialRecordRequest;
import com.zorvyn.finance.dto.request.UpdateFinancialRecordRequest;
import com.zorvyn.finance.dto.response.FinancialRecordResponse;
import com.zorvyn.common.dto.PagedResponse;
import com.zorvyn.finance.entity.FinancialRecord;
import com.zorvyn.finance.entity.RecordType;
import com.zorvyn.finance.exception.BadRequestException;
import com.zorvyn.finance.exception.ResourceNotFoundException;
import com.zorvyn.finance.mapper.FinancialRecordMapper;
import com.zorvyn.finance.repository.FinancialRecordRepository;
import com.zorvyn.finance.service.FinancialRecordService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class FinancialRecordServiceImpl implements FinancialRecordService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
        "id",
        "amount",
        "type",
        "category",
        "transactionDate",
        "createdAt",
        "updatedAt"
    );

    private final FinancialRecordRepository financialRecordRepository;

    public FinancialRecordServiceImpl(
        FinancialRecordRepository financialRecordRepository
    ) {
        this.financialRecordRepository = financialRecordRepository;
    }

    @Override
    @CacheEvict(cacheNames = CacheNames.DASHBOARD_SUMMARY, allEntries = true)
    public FinancialRecordResponse createRecord(
        CreateFinancialRecordRequest request
    ) {
        FinancialRecord record = new FinancialRecord();
        record.setAmount(request.amount());
        record.setType(request.type());
        record.setCategory(normalizeCategory(request.category()));
        record.setDescription(normalizeDescription(request.description()));
        record.setTransactionDate(request.transactionDate());
        record.setDeleted(false);

        return FinancialRecordMapper.toResponse(
            financialRecordRepository.save(record)
        );
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheNames.RECORD_BY_ID, key = "#id")
    public FinancialRecordResponse getRecordById(Long id) {
        return FinancialRecordMapper.toResponse(findRecordById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<FinancialRecordResponse> listRecords(
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
    ) {
        validateAmountRange(minAmount, maxAmount);
        validateDateRange(dateFrom, dateTo);

        Pageable pageable = PageRequest.of(
            page,
            size,
            Sort.by(resolveSortDirection(sortDir), resolveSortBy(sortBy))
        );

        Specification<FinancialRecord> specification = buildSpecification(
            type,
            category,
            dateFrom,
            dateTo,
            minAmount,
            maxAmount,
            search
        );

        Page<FinancialRecordResponse> result = financialRecordRepository
            .findAll(specification, pageable)
            .map(FinancialRecordMapper::toResponse);

        return PagedResponse.from(result);
    }

    @Override
    @Caching(
        evict = {
            @CacheEvict(
                cacheNames = CacheNames.DASHBOARD_SUMMARY,
                allEntries = true
            ),
            @CacheEvict(cacheNames = CacheNames.RECORD_BY_ID, key = "#id"),
        }
    )
    public FinancialRecordResponse updateRecord(
        Long id,
        UpdateFinancialRecordRequest request
    ) {
        FinancialRecord record = findRecordById(id);

        if (!hasAnyUpdate(request)) {
            throw new BadRequestException(
                "At least one field must be provided to update"
            );
        }

        if (request.amount() != null) {
            record.setAmount(request.amount());
        }

        if (request.type() != null) {
            record.setType(request.type());
        }

        if (request.category() != null) {
            record.setCategory(normalizeCategory(request.category()));
        }

        if (request.description() != null) {
            record.setDescription(normalizeDescription(request.description()));
        }

        if (request.transactionDate() != null) {
            record.setTransactionDate(request.transactionDate());
        }

        return FinancialRecordMapper.toResponse(
            financialRecordRepository.save(record)
        );
    }

    @Override
    @Caching(
        evict = {
            @CacheEvict(
                cacheNames = CacheNames.DASHBOARD_SUMMARY,
                allEntries = true
            ),
            @CacheEvict(cacheNames = CacheNames.RECORD_BY_ID, key = "#id"),
        }
    )
    public void deleteRecord(Long id) {
        FinancialRecord record = findRecordById(id);
        record.setDeleted(true);
        record.setDeletedAt(LocalDateTime.now());
        financialRecordRepository.save(record);
    }

    private FinancialRecord findRecordById(Long id) {
        return financialRecordRepository
            .findByIdAndDeletedFalse(id)
            .orElseThrow(() ->
                new ResourceNotFoundException(
                    "Financial record not found with id=" + id
                )
            );
    }

    private Specification<FinancialRecord> buildSpecification(
        RecordType type,
        String category,
        LocalDate dateFrom,
        LocalDate dateTo,
        BigDecimal minAmount,
        BigDecimal maxAmount,
        String search
    ) {
        Specification<FinancialRecord> specification = (root, query, cb) ->
            cb.isFalse(root.get("deleted"));

        if (type != null) {
            specification = specification.and((root, query, cb) ->
                cb.equal(root.get("type"), type)
            );
        }

        if (hasText(category)) {
            String normalizedCategory = category
                .trim()
                .toLowerCase(Locale.ROOT);
            specification = specification.and((root, query, cb) ->
                cb.equal(cb.lower(root.get("category")), normalizedCategory)
            );
        }

        if (dateFrom != null) {
            specification = specification.and((root, query, cb) ->
                cb.greaterThanOrEqualTo(root.get("transactionDate"), dateFrom)
            );
        }

        if (dateTo != null) {
            specification = specification.and((root, query, cb) ->
                cb.lessThanOrEqualTo(root.get("transactionDate"), dateTo)
            );
        }

        if (minAmount != null) {
            specification = specification.and((root, query, cb) ->
                cb.greaterThanOrEqualTo(root.get("amount"), minAmount)
            );
        }

        if (maxAmount != null) {
            specification = specification.and((root, query, cb) ->
                cb.lessThanOrEqualTo(root.get("amount"), maxAmount)
            );
        }

        if (hasText(search)) {
            String pattern = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
            specification = specification.and((root, query, cb) ->
                cb.or(
                    cb.like(cb.lower(root.get("category")), pattern),
                    cb.like(cb.lower(root.get("description")), pattern)
                )
            );
        }

        return specification;
    }

    private String resolveSortBy(String sortBy) {
        String resolved = hasText(sortBy) ? sortBy.trim() : "transactionDate";
        if (!ALLOWED_SORT_FIELDS.contains(resolved)) {
            throw new BadRequestException(
                "Invalid sortBy field. Allowed values: " +
                    List.copyOf(ALLOWED_SORT_FIELDS)
            );
        }
        return resolved;
    }

    private Sort.Direction resolveSortDirection(String sortDir) {
        try {
            return hasText(sortDir)
                ? Sort.Direction.fromString(sortDir.trim())
                : Sort.Direction.DESC;
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException(
                "Invalid sortDir. Allowed values: ASC or DESC"
            );
        }
    }

    private void validateAmountRange(
        BigDecimal minAmount,
        BigDecimal maxAmount
    ) {
        if (
            minAmount != null &&
            maxAmount != null &&
            minAmount.compareTo(maxAmount) > 0
        ) {
            throw new BadRequestException(
                "minAmount cannot be greater than maxAmount"
            );
        }
    }

    private void validateDateRange(LocalDate dateFrom, LocalDate dateTo) {
        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            throw new BadRequestException("dateFrom cannot be after dateTo");
        }
    }

    private boolean hasAnyUpdate(UpdateFinancialRecordRequest request) {
        return (
            request.amount() != null ||
            request.type() != null ||
            request.category() != null ||
            request.description() != null ||
            request.transactionDate() != null
        );
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String normalizeCategory(String category) {
        return category.trim();
    }

    private String normalizeDescription(String description) {
        return hasText(description) ? description.trim() : null;
    }
}
