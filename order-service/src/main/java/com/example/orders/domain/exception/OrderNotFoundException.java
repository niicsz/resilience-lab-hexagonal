package com.example.orders.domain.exception;

public class OrderNotFoundException extends RuntimeException {
  public OrderNotFoundException(String publicId) {
    super("order " + publicId + " not found");
  }
}
