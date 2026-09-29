package com.example.orders.application.ports.outbound;

import com.example.orders.domain.Order;
import com.example.orders.domain.OrderStatus;
import java.util.List;
import java.util.Optional;

public interface OrderRepositoryPort {
  Order save(Order order);

  Optional<Order> findByPublicId(String publicId);

  List<Order> findByStatus(OrderStatus status);
}
