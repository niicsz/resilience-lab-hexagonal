package com.example.orders.domain.exception;

public class PaymentDeclinedException extends RuntimeException {
  public PaymentDeclinedException(String message) {
    super(message);
  }
}
