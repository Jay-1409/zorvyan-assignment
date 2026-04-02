package com.zorvyn.finance.exception;

import com.zorvyn.common.dto.ApiErrorResponse;
import com.zorvyn.common.dto.ValidationErrorDetail;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(
        GlobalExceptionHandler.class
    );

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiErrorResponse> handleApiException(
        ApiException ex,
        HttpServletRequest request
    ) {
        return ResponseEntity.status(ex.getStatus()).body(
            buildError(
                ex.getStatus(),
                ex.getCode(),
                ex.getMessage(),
                request.getRequestURI(),
                List.of()
            )
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(
        MethodArgumentNotValidException ex,
        HttpServletRequest request
    ) {
        List<ValidationErrorDetail> validationErrors = new ArrayList<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            validationErrors.add(
                new ValidationErrorDetail(
                    fieldError.getField(),
                    fieldError.getDefaultMessage()
                )
            );
        }

        ApiErrorResponse response = buildError(
            HttpStatus.BAD_REQUEST,
            "VALIDATION_FAILED",
            "Request validation failed",
            request.getRequestURI(),
            validationErrors
        );

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(
        {
            ConstraintViolationException.class,
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
        }
    )
    public ResponseEntity<ApiErrorResponse> handleBadRequest(
        Exception ex,
        HttpServletRequest request
    ) {
        ApiErrorResponse response = buildError(
            HttpStatus.BAD_REQUEST,
            "BAD_REQUEST",
            ex.getMessage(),
            request.getRequestURI(),
            List.of()
        );
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(
        { AccessDeniedException.class, AuthorizationDeniedException.class }
    )
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(
        Exception ex,
        HttpServletRequest request
    ) {
        ApiErrorResponse response = buildError(
            HttpStatus.FORBIDDEN,
            "FORBIDDEN",
            "You do not have permission to perform this action",
            request.getRequestURI(),
            List.of()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException(
        Exception ex,
        HttpServletRequest request
    ) {
        LOGGER.error(
            "Unhandled exception at path={}",
            request.getRequestURI(),
            ex
        );

        ApiErrorResponse response = buildError(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "INTERNAL_SERVER_ERROR",
            "An unexpected error occurred",
            request.getRequestURI(),
            List.of()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            response
        );
    }

    private ApiErrorResponse buildError(
        HttpStatus status,
        String code,
        String message,
        String path,
        List<ValidationErrorDetail> validationErrors
    ) {
        return new ApiErrorResponse(
            Instant.now(),
            status.value(),
            status.getReasonPhrase(),
            code,
            message,
            path,
            validationErrors
        );
    }
}
