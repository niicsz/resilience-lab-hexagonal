package com.example.orders.domain;

import com.example.orders.domain.exception.BusinessException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

public class Order {
  private static final Set<OrderStatus> AWAITING_PAYMENT =
      EnumSet.of(OrderStatus.PENDING, OrderStatus.PAYMENT_PENDING);

  private Long id;
  private String publicId;
  private String sku;
  private int quantity;
  private BigDecimal amount;
  private String currency;
  private OrderStatus status;
  private Instant createdAt;
  private Instant updatedAt;

  private Order() {}

  public static Order place(String sku, int quantity, BigDecimal amount, String currency) {
    if (sku == null || sku.isBlank()) throw new BusinessException("sku is required");
    if (quantity <= 0) throw new BusinessException("quantity must be positive");
    if (amount == null || amount.signum() <= 0)
      throw new BusinessException("amount must be positive");
    if (currency == null || currency.isBlank()) throw new BusinessException("currency is required");

    Order order = new Order();
    order.publicId = UUID.randomUUID().toString();
    order.sku = sku;
    order.quantity = quantity;
    order.amount = amount;
    order.currency = currency;
    order.status = OrderStatus.PENDING;
    return order;
  }

  public static Order restore(
      Long id,
      String publicId,
      String sku,
      int quantity,
      BigDecimal amount,
      String currency,
      OrderStatus status,
      Instant createdAt,
      Instant updatedAt) {
    Order order = new Order();
    order.id = id;
    order.publicId = publicId;
    order.sku = sku;
    order.quantity = quantity;
    order.amount = amount;
    order.currency = currency;
    order.status = status;
    order.createdAt = createdAt;
    order.updatedAt = updatedAt;
    return order;
  }

  public void markOutOfStock() {
    if (status != OrderStatus.PENDING)
      throw new IllegalStateException("cannot mark order " + publicId + " out of stock: " + status);
    status = OrderStatus.OUT_OF_STOCK;
  }

  public void applyPayment(PaymentOutcome outcome) {
    if (!AWAITING_PAYMENT.contains(status))
      throw new IllegalStateException("order " + publicId + " is not awaiting payment: " + status);
    status =
        switch (outcome) {
          case APPROVED -> OrderStatus.CONFIRMED;
          case DECLINED -> OrderStatus.PAYMENT_FAILED;
          case PENDING -> OrderStatus.PAYMENT_PENDING;
        };
  }

  public Long getId() {
    return id;
  }

  public String getPublicId() {
    return publicId;
  }

  public String getSku() {
    return sku;
  }

  public int getQuantity() {
    return quantity;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public String getCurrency() {
    return currency;
  }

  public OrderStatus getStatus() {
    return status;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
