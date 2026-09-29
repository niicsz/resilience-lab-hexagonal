package com.example.orders.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.orders.application.ports.inbound.CreateOrderCommand;
import com.example.orders.application.ports.outbound.InventoryPort;
import com.example.orders.application.ports.outbound.NotificationPort;
import com.example.orders.application.ports.outbound.PaymentGatewayPort;
import com.example.orders.application.ports.outbound.TrackingPort;
import com.example.orders.domain.Order;
import com.example.orders.domain.OrderStatus;
import com.example.orders.domain.PaymentOutcome;
import com.example.orders.domain.TrackingInfo;
import com.example.orders.domain.exception.ExternalServiceUnavailableException;
import com.example.orders.domain.exception.OrderNotFoundException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class OrderUseCaseTest {
  private final InMemoryOrderRepository orders = new InMemoryOrderRepository();
  private final List<String> notified = new ArrayList<>();
  private InventoryPort inventory = order -> true;
  private PaymentGatewayPort payments = order -> PaymentOutcome.APPROVED;
  private NotificationPort notifications = order -> notified.add(order.getPublicId());
  private TrackingPort tracking = id -> new TrackingInfo(id, "ACME", "XYZ", "IN_TRANSIT");

  private OrderUseCase useCase() {
    return new OrderUseCase(orders, inventory, payments, notifications, tracking);
  }

  private Order create() {
    return useCase().create(new CreateOrderCommand("SKU-1", 2, new BigDecimal("19.90"), "BRL"));
  }

  private static ExternalServiceUnavailableException unavailable() {
    return new ExternalServiceUnavailableException("down", null);
  }

  @Test
  void approvedPaymentConfirmsAndNotifies() {
    Order order = create();

    assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    assertThat(order.getId()).isNotNull();
    assertThat(notified).containsExactly(order.getPublicId());
    assertThat(orders.findByPublicId(order.getPublicId()))
        .get()
        .extracting(Order::getStatus)
        .isEqualTo(OrderStatus.CONFIRMED);
  }

  @Test
  void notReservedBecomesOutOfStockWithoutCharging() {
    inventory = order -> false;
    payments =
        order -> {
          throw new AssertionError("must not charge an order without stock");
        };

    Order order = create();

    assertThat(order.getStatus()).isEqualTo(OrderStatus.OUT_OF_STOCK);
    assertThat(notified).isEmpty();
  }

  @Test
  void unavailableInventoryBecomesOutOfStock() {
    inventory =
        order -> {
          throw unavailable();
        };

    assertThat(create().getStatus()).isEqualTo(OrderStatus.OUT_OF_STOCK);
  }

  @Test
  void unavailablePaymentBecomesPaymentPendingAndStillNotifies() {
    payments =
        order -> {
          throw unavailable();
        };

    Order order = create();

    assertThat(order.getStatus()).isEqualTo(OrderStatus.PAYMENT_PENDING);
    assertThat(notified).containsExactly(order.getPublicId());
  }

  @Test
  void declinedPaymentFailsWithoutNotifying() {
    payments = order -> PaymentOutcome.DECLINED;

    assertThat(create().getStatus()).isEqualTo(OrderStatus.PAYMENT_FAILED);
    assertThat(notified).isEmpty();
  }

  @Test
  void notificationFailureWithoutFallbackBreaksTheOrder() {
    notifications =
        order -> {
          throw unavailable();
        };

    assertThatThrownBy(this::create).isInstanceOf(ExternalServiceUnavailableException.class);
    assertThat(orders.all())
        .singleElement()
        .extracting(Order::getStatus)
        .isEqualTo(OrderStatus.PENDING);
  }

  @Test
  void getReturnsPersistedOrder() {
    Order created = create();

    assertThat(useCase().get(created.getPublicId()).getStatus()).isEqualTo(OrderStatus.CONFIRMED);
  }

  @Test
  void getThrowsWhenMissing() {
    assertThatThrownBy(() -> useCase().get("missing")).isInstanceOf(OrderNotFoundException.class);
  }

  @Test
  void trackDelegatesToTrackingPort() {
    assertThat(useCase().track("ORD-1").state()).isEqualTo("IN_TRANSIT");
  }
}
