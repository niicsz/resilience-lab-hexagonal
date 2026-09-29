package com.example.orders.application.ports.outbound;

import com.example.orders.domain.Order;

public interface NotificationPort {
  void orderUpdated(Order order);
}
