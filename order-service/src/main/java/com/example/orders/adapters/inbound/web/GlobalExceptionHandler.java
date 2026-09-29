package com.example.orders.adapters.inbound.web;

import com.example.orders.domain.exception.BusinessException;
import com.example.orders.domain.exception.DependencyOverloadedException;
import com.example.orders.domain.exception.ExternalServiceUnavailableException;
import com.example.orders.domain.exception.OrderNotFoundException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<Map<String, String>> validation(MethodArgumentNotValidException exception) {
    var errors = exception.getBindingResult().getAllErrors();
    return body(
        HttpStatus.BAD_REQUEST,
        "validation_error",
        errors.isEmpty() ? "invalid request" : errors.getFirst().getDefaultMessage());
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  ResponseEntity<Map<String, String>> unreadable(HttpMessageNotReadableException exception) {
    return body(HttpStatus.BAD_REQUEST, "validation_error", "malformed request body");
  }

  @ExceptionHandler(BusinessException.class)
  ResponseEntity<Map<String, String>> business(BusinessException exception) {
    return body(HttpStatus.BAD_REQUEST, "validation_error", exception.getMessage());
  }

  @ExceptionHandler(OrderNotFoundException.class)
  ResponseEntity<Map<String, String>> notFound(OrderNotFoundException exception) {
    return body(HttpStatus.NOT_FOUND, "order_not_found", exception.getMessage());
  }

  @ExceptionHandler(DependencyOverloadedException.class)
  ResponseEntity<Map<String, String>> overloaded(DependencyOverloadedException exception) {
    return body(
        HttpStatus.SERVICE_UNAVAILABLE,
        exception.getDependency() + "_overloaded",
        exception.getMessage());
  }

  @ExceptionHandler(ExternalServiceUnavailableException.class)
  ResponseEntity<Map<String, String>> unavailable(ExternalServiceUnavailableException exception) {
    return body(HttpStatus.SERVICE_UNAVAILABLE, "dependency_unavailable", exception.getMessage());
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<Map<String, String>> other(Exception exception) {
    if (exception instanceof ErrorResponse errorResponse) {
      HttpStatusCode status = errorResponse.getStatusCode();
      return body(status, "request_error", errorResponse.getBody().getDetail());
    }
    LOGGER.error("unexpected error", exception);
    return body(HttpStatus.INTERNAL_SERVER_ERROR, "internal_error", "unexpected error");
  }

  private ResponseEntity<Map<String, String>> body(
      HttpStatusCode status, String error, String message) {
    return ResponseEntity.status(status)
        .body(Map.of("error", error, "message", message == null ? "" : message));
  }
}
