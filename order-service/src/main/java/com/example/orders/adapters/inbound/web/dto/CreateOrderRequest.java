package com.example.orders.adapters.inbound.web.dto;

import com.example.orders.application.ports.inbound.CreateOrderCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record CreateOrderRequest(
    @NotBlank String sku,
    @Positive int quantity,
    @NotNull @Positive BigDecimal amount,
    @NotBlank String currency) {

  public CreateOrderCommand toCommand() {
    return new CreateOrderCommand(sku, quantity, amount, currency);
  }
}
