package com.logistics.shipmenttracker.exception;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** Converts every failure into the same JSON shape without leaking stack traces or SQL. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ShipmentNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ShipmentNotFoundException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), req, null);
    }

    @ExceptionHandler(DuplicateTrackingNumberException.class)
    public ResponseEntity<ApiError> handleDuplicate(DuplicateTrackingNumberException ex, HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), req,
                Map.of("trackingNumber", "Tracking number already exists"));
    }

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ApiError> handleInvalid(InvalidRequestException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), req, Map.of(ex.getField(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(fe ->
                // a blank value triggers both @NotBlank and @Size; prefer the "required" message
                fieldErrors.merge(fe.getField(), String.valueOf(fe.getDefaultMessage()),
                        (a, b) -> a.contains("required") ? a : b));
        return build(HttpStatus.BAD_REQUEST, "Validation failed", req, fieldErrors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex, HttpServletRequest req) {
        if (ex.getCause() instanceof InvalidFormatException ife && !ife.getPath().isEmpty()) {
            String field = ife.getPath().get(ife.getPath().size() - 1).getFieldName();
            String message;
            if (ife.getTargetType() != null && ife.getTargetType().isEnum()) {
                message = "Invalid value for '" + field + "'. Allowed values: " + Arrays.stream(
                        ife.getTargetType().getEnumConstants()).map(Object::toString).collect(Collectors.joining(", "));
            } else {
                message = "Invalid value for '" + field + "'";
            }
            return build(HttpStatus.BAD_REQUEST, message, req, Map.of(field, message));
        }
        return build(HttpStatus.BAD_REQUEST, "Malformed or unreadable request body", req, null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
        String message = "Invalid value for parameter '" + ex.getName() + "'";
        Class<?> type = ex.getRequiredType();
        if (type != null && type.isEnum()) {
            message += ". Allowed values: " + Arrays.stream(type.getEnumConstants())
                    .map(Object::toString).collect(Collectors.joining(", "));
        }
        return build(HttpStatus.BAD_REQUEST, message, req, null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex, HttpServletRequest req) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, "HTTP method not supported for this endpoint", req, null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNoResource(NoResourceFoundException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, "Resource not found", req, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest req) {
        log.error("Unexpected error on {} {}", req.getMethod(), req.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred. Please try again later.", req, null);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message, HttpServletRequest req,
                                           Map<String, String> fieldErrors) {
        ApiError body = new ApiError(Instant.now(), status.value(), status.getReasonPhrase(),
                message, req.getRequestURI(), fieldErrors);
        return ResponseEntity.status(status).body(body);
    }
}
