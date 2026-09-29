package com.example.orders.adapters.outbound.persistence;

import com.example.orders.adapters.outbound.persistence.entity.OrderEntity;
import com.example.orders.adapters.outbound.persistence.repository.SpringDataOrderRepository;
import com.example.orders.application.ports.outbound.OrderRepositoryPort;
import com.example.orders.domain.Order;
import com.example.orders.domain.OrderStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class OrderRepositoryAdapter implements OrderRepositoryPort {
  private final SpringDataOrderRepository repository;

  public OrderRepositoryAdapter(SpringDataOrderRepository repository) {
    this.repository = repository;
  }

  @Override
  public Order save(Order order) {
    return repository.save(OrderEntity.fromDomain(order)).toDomain();
  }

  @Override
  public Optional<Order> findByPublicId(String publicId) {
    return repository.findByPublicId(publicId).map(OrderEntity::toDomain);
  }

  @Override
  public List<Order> findByStatus(OrderStatus status) {
    return repository.findByStatus(status).stream().map(OrderEntity::toDomain).toList();
  }
}
