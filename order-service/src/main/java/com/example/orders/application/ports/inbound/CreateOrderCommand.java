package com.example.orders.application.ports.inbound;

import java.math.BigDecimal;

public record CreateOrderCommand(String sku, int quantity, BigDecimal amount, String currency) {}
