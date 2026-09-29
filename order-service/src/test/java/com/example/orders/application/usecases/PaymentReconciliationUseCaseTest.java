package com.example.orders.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.orders.application.ports.outbound.PaymentGatewayPort;
import com.example.orders.domain.Order;
import com.example.orders.domain.OrderStatus;
import com.example.orders.domain.PaymentOutcome;
import com.example.orders.domain.exception.ExternalServiceUnavailableException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PaymentReconciliationUseCaseTest {
  private final InMemoryOrderRepository orders = new InMemoryOrderRepository();
  private final List<String> charged = new ArrayList<>();
  private Order pending;

  @BeforeEach
  void setUp() {
    Order order = Order.place("SKU-1", 1, new BigDecimal("10.00"), "BRL");
    order.applyPayment(PaymentOutcome.PENDING);
    pending = orders.save(order);

    Order confirmed = Order.place("SKU-2", 1, new BigDecimal("10.00"), "BRL");
    confirmed.applyPayment(PaymentOutcome.APPROVED);
    orders.save(confirmed);
    orders.saves = 0;
  }

  private void reconcileWith(PaymentOutcome outcome) {
    reconcileWith(
        order -> {
          charged.add(order.getPublicId());
          return outcome;
        });
  }

  private void reconcileWith(PaymentGatewayPort payments) {
    new PaymentReconciliationUseCase(orders, payments).reconcilePending();
  }

  private OrderStatus statusOfPending() {
    return orders.findByPublicId(pending.getPublicId()).orElseThrow().getStatus();
  }

  @Test
  void approvedConfirmsOnlyPendingOrders() {
    reconcileWith(PaymentOutcome.APPROVED);

    assertThat(statusOfPending()).isEqualTo(OrderStatus.CONFIRMED);
    assertThat(charged).containsExactly(pending.getPublicId());
  }

  @Test
  void declinedFailsThePayment() {
    reconcileWith(PaymentOutcome.DECLINED);

    assertThat(statusOfPending()).isEqualTo(OrderStatus.PAYMENT_FAILED);
  }

  @Test
  void stillPendingIsLeftForTheNextRun() {
    reconcileWith(PaymentOutcome.PENDING);

    assertThat(statusOfPending()).isEqualTo(OrderStatus.PAYMENT_PENDING);
    assertThat(orders.saves).isZero();
  }

  @Test
  void unavailableGatewayIsLeftForTheNextRun() {
    reconcileWith(
        order -> {
          throw new ExternalServiceUnavailableException("down", null);
        });

    assertThat(statusOfPending()).isEqualTo(OrderStatus.PAYMENT_PENDING);
    assertThat(orders.saves).isZero();
  }
}
