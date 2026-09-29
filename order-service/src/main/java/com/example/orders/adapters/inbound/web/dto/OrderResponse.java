package com.example.orders.adapters.inbound.web.dto;

import com.example.orders.domain.Order;
import java.math.BigDecimal;

public record OrderResponse(
    String orderId, String status, String sku, int quantity, BigDecimal amount, String currency) {

  public static OrderResponse from(Order order) {
    return new OrderResponse(
        order.getPublicId(),
        order.getStatus().name(),
        order.getSku(),
        order.getQuantity(),
        order.getAmount(),
        order.getCurrency());
  }
}
