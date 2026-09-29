package com.example.orders.adapters.outbound.persistence.entity;

import com.example.orders.domain.Order;
import com.example.orders.domain.OrderStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "orders")
public class OrderEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "public_id", nullable = false, unique = true, length = 36)
  private String publicId;

  @Column(nullable = false, length = 64)
  private String sku;

  @Column(nullable = false)
  private int quantity;

  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal amount;

  @Column(nullable = false, length = 3)
  private String currency;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private OrderStatus status;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected OrderEntity() {}

  public static OrderEntity fromDomain(Order order) {
    OrderEntity entity = new OrderEntity();
    entity.id = order.getId();
    entity.publicId = order.getPublicId();
    entity.sku = order.getSku();
    entity.quantity = order.getQuantity();
    entity.amount = order.getAmount();
    entity.currency = order.getCurrency();
    entity.status = order.getStatus();
    entity.createdAt = order.getCreatedAt();
    entity.updatedAt = order.getUpdatedAt();
    return entity;
  }

  public Order toDomain() {
    return Order.restore(
        id, publicId, sku, quantity, amount, currency, status, createdAt, updatedAt);
  }

  @PrePersist
  void onCreate() {
    Instant now = Instant.now();
    if (createdAt == null) createdAt = now;
    updatedAt = now;
  }

  @PreUpdate
  void onUpdate() {
    updatedAt = Instant.now();
  }
}
