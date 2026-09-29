package com.example.orders.application.usecases;

import com.example.orders.application.ports.outbound.OrderRepositoryPort;
import com.example.orders.domain.Order;
import com.example.orders.domain.OrderStatus;
import java.time.Instant;
import java.util.*;

class InMemoryOrderRepository implements OrderRepositoryPort {
  private final Map<Long, Order> rows = new LinkedHashMap<>();
  private long sequence;
  int saves;

  @Override
  public Order save(Order order) {
    saves++;
    Long id = order.getId() != null ? order.getId() : ++sequence;
    Instant createdAt = order.getCreatedAt() != null ? order.getCreatedAt() : Instant.now();
    rows.put(id, copy(order, id, createdAt));
    return copy(rows.get(id), id, createdAt);
  }

  @Override
  public Optional<Order> findByPublicId(String publicId) {
    return rows.values().stream().filter(o -> o.getPublicId().equals(publicId)).findFirst();
  }

  @Override
  public List<Order> findByStatus(OrderStatus status) {
    return rows.values().stream()
        .filter(o -> o.getStatus() == status)
        .map(o -> copy(o, o.getId(), o.getCreatedAt()))
        .toList();
  }

  List<Order> all() {
    return List.copyOf(rows.values());
  }

  private static Order copy(Order order, Long id, Instant createdAt) {
    return Order.restore(
        id,
        order.getPublicId(),
        order.getSku(),
        order.getQuantity(),
        order.getAmount(),
        order.getCurrency(),
        order.getStatus(),
        createdAt,
        Instant.now());
  }
}
