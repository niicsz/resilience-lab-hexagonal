package com.example.orders.adapters.outbound.persistence.repository;

import com.example.orders.adapters.outbound.persistence.entity.OrderEntity;
import com.example.orders.domain.OrderStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataOrderRepository extends JpaRepository<OrderEntity, Long> {
  Optional<OrderEntity> findByPublicId(String publicId);

  List<OrderEntity> findByStatus(OrderStatus status);
}
