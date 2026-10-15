package com.firstclub.membership.service.controller;

import com.firstclub.membership.commons.ConflictException;
import com.firstclub.membership.commons.ResourceNotFoundException;
import com.firstclub.membership.models.common.ApiErrorResponse;
import com.firstclub.membership.models.common.FieldViolationResponse;
import java.time.Instant;
import java.util.List;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

@RestControllerAdvice
public class ApiExceptionHandler {
  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ApiErrorResponse> handleNotFound(ResourceNotFoundException exception) {
    return error(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", exception.getMessage(), List.of());
  }

  @ExceptionHandler(ConflictException.class)
  public ResponseEntity<ApiErrorResponse> handleConflict(ConflictException exception) {
    return error(HttpStatus.CONFLICT, "CONFLICT", exception.getMessage(), List.of());
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ApiErrorResponse> handleDataIntegrityViolation(
      DataIntegrityViolationException exception) {
    return error(
        HttpStatus.CONFLICT,
        "DATA_INTEGRITY_VIOLATION",
        "The operation conflicts with persisted membership data",
        List.of());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiErrorResponse> handleRequestBodyValidation(
      MethodArgumentNotValidException exception) {
    return error(
        HttpStatus.BAD_REQUEST,
        "VALIDATION_FAILED",
        "One or more request fields are invalid",
        exception.getBindingResult().getFieldErrors().stream()
            .map(this::toFieldViolation)
            .toList());
  }

  @ExceptionHandler(HandlerMethodValidationException.class)
  public ResponseEntity<ApiErrorResponse> handleMethodValidation(
      HandlerMethodValidationException exception) {
    List<FieldViolationResponse> violations =
        exception.getParameterValidationResults().stream()
            .flatMap(
                result ->
                    result.getResolvableErrors().stream()
                        .map(
                            error ->
                                toFieldViolation(
                                    result.getMethodParameter().getParameterName(), error)))
            .toList();
    return error(
        HttpStatus.BAD_REQUEST,
        "VALIDATION_FAILED",
        "One or more request fields are invalid",
        violations);
  }

  private FieldViolationResponse toFieldViolation(FieldError error) {
    return toFieldViolation(error.getField(), error);
  }

  private FieldViolationResponse toFieldViolation(String field, MessageSourceResolvable error) {
    return new FieldViolationResponse(
        field == null || field.isBlank() ? "request" : field,
        error.getDefaultMessage() == null ? "Invalid value" : error.getDefaultMessage());
  }

  private ResponseEntity<ApiErrorResponse> error(
      HttpStatus status, String code, String message, List<FieldViolationResponse> violations) {
    return ResponseEntity.status(status)
        .body(new ApiErrorResponse(code, message, violations, Instant.now()));
  }
}
