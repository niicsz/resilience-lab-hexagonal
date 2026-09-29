package com.example.orders.application.usecases;

import com.example.orders.application.ports.inbound.CreateOrderCommand;
import com.example.orders.application.ports.inbound.OrderUseCasePort;
import com.example.orders.application.ports.outbound.*;
import com.example.orders.domain.Order;
import com.example.orders.domain.OrderStatus;
import com.example.orders.domain.PaymentOutcome;
import com.example.orders.domain.TrackingInfo;
import com.example.orders.domain.exception.ExternalServiceUnavailableException;
import com.example.orders.domain.exception.OrderNotFoundException;

public class OrderUseCase implements OrderUseCasePort {
  private final OrderRepositoryPort orders;
  private final InventoryPort inventory;
  private final PaymentGatewayPort payments;
  private final NotificationPort notifications;
  private final TrackingPort tracking;

  public OrderUseCase(
      OrderRepositoryPort orders,
      InventoryPort inventory,
      PaymentGatewayPort payments,
      NotificationPort notifications,
      TrackingPort tracking) {
    this.orders = orders;
    this.inventory = inventory;
    this.payments = payments;
    this.notifications = notifications;
    this.tracking = tracking;
  }

  @Override
  public Order create(CreateOrderCommand command) {
    Order order =
        orders.save(
            Order.place(command.sku(), command.quantity(), command.amount(), command.currency()));

    if (!reserveInventory(order)) {
      order.markOutOfStock();
      return orders.save(order);
    }

    order.applyPayment(charge(order));
    if (order.getStatus() != OrderStatus.PAYMENT_FAILED) {
      notifications.orderUpdated(order);
    }
    return orders.save(order);
  }

  @Override
  public Order get(String publicId) {
    return orders.findByPublicId(publicId).orElseThrow(() -> new OrderNotFoundException(publicId));
  }

  @Override
  public TrackingInfo track(String publicId) {
    return tracking.track(publicId);
  }

  private boolean reserveInventory(Order order) {
    try {
      return inventory.reserve(order);
    } catch (ExternalServiceUnavailableException exception) {
      return false;
    }
  }

  private PaymentOutcome charge(Order order) {
    try {
      return payments.charge(order);
    } catch (ExternalServiceUnavailableException exception) {
      return PaymentOutcome.PENDING;
    }
  }
}
