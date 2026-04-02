package com.zorvyn.common.dto;

import java.time.Instant;
import java.util.List;

public record ApiErrorResponse(
    Instant timestamp,
    int status,
    String error,
    String code,
    String message,
    String path,
    List<ValidationErrorDetail> validationErrors
) {}
