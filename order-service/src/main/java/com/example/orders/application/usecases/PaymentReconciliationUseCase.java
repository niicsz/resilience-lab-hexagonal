package com.example.orders.application.usecases;

import com.example.orders.application.ports.inbound.PaymentReconciliationUseCasePort;
import com.example.orders.application.ports.outbound.OrderRepositoryPort;
import com.example.orders.application.ports.outbound.PaymentGatewayPort;
import com.example.orders.domain.Order;
import com.example.orders.domain.OrderStatus;
import com.example.orders.domain.PaymentOutcome;
import com.example.orders.domain.exception.ExternalServiceUnavailableException;

public class PaymentReconciliationUseCase implements PaymentReconciliationUseCasePort {
  private final OrderRepositoryPort orders;
  private final PaymentGatewayPort payments;

  public PaymentReconciliationUseCase(OrderRepositoryPort orders, PaymentGatewayPort payments) {
    this.orders = orders;
    this.payments = payments;
  }

  @Override
  public void reconcilePending() {
    for (Order order : orders.findByStatus(OrderStatus.PAYMENT_PENDING)) {
      PaymentOutcome outcome;
      try {
        outcome = payments.charge(order);
      } catch (ExternalServiceUnavailableException exception) {
        continue;
      }
      if (outcome != PaymentOutcome.PENDING) {
        order.applyPayment(outcome);
        orders.save(order);
      }
    }
  }
}
