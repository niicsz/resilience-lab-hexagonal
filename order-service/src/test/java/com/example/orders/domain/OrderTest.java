package com.example.orders.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.orders.domain.exception.BusinessException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class OrderTest {

  private Order placed() {
    return Order.place("SKU-1", 2, new BigDecimal("19.90"), "BRL");
  }

  @Test
  void placedOrderStartsPendingWithPublicId() {
    Order order = placed();

    assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
    assertThat(order.getPublicId()).hasSize(36);
    assertThat(order.getId()).isNull();
  }

  @Test
  void rejectsInvalidData() {
    assertThatThrownBy(() -> Order.place(" ", 1, BigDecimal.ONE, "BRL"))
        .isInstanceOf(BusinessException.class);
    assertThatThrownBy(() -> Order.place("SKU", 0, BigDecimal.ONE, "BRL"))
        .isInstanceOf(BusinessException.class);
    assertThatThrownBy(() -> Order.place("SKU", 1, BigDecimal.ZERO, "BRL"))
        .isInstanceOf(BusinessException.class);
    assertThatThrownBy(() -> Order.place("SKU", 1, BigDecimal.ONE, null))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  void paymentOutcomeDrivesStatus() {
    Order approved = placed();
    approved.applyPayment(PaymentOutcome.APPROVED);
    Order declined = placed();
    declined.applyPayment(PaymentOutcome.DECLINED);
    Order pending = placed();
    pending.applyPayment(PaymentOutcome.PENDING);

    assertThat(approved.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    assertThat(declined.getStatus()).isEqualTo(OrderStatus.PAYMENT_FAILED);
    assertThat(pending.getStatus()).isEqualTo(OrderStatus.PAYMENT_PENDING);
  }

  @Test
  void pendingPaymentCanBeReconciled() {
    Order order = placed();
    order.applyPayment(PaymentOutcome.PENDING);

    order.applyPayment(PaymentOutcome.APPROVED);

    assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
  }

  @Test
  void finalStatesCannotChange() {
    Order confirmed = placed();
    confirmed.applyPayment(PaymentOutcome.APPROVED);
    Order outOfStock = placed();
    outOfStock.markOutOfStock();

    assertThatThrownBy(() -> confirmed.applyPayment(PaymentOutcome.DECLINED))
        .isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(confirmed::markOutOfStock).isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(() -> outOfStock.applyPayment(PaymentOutcome.APPROVED))
        .isInstanceOf(IllegalStateException.class);
  }
}
