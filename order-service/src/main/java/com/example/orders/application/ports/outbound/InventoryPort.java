package com.example.orders.application.ports.outbound;

import com.example.orders.domain.Order;

public interface InventoryPort {
  boolean reserve(Order order);
}
