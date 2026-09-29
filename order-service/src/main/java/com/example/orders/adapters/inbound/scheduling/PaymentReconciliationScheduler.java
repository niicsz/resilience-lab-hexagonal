package com.example.orders.adapters.inbound.scheduling;

import com.example.orders.application.ports.inbound.PaymentReconciliationUseCasePort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PaymentReconciliationScheduler {
  private final PaymentReconciliationUseCasePort reconciliation;

  public PaymentReconciliationScheduler(PaymentReconciliationUseCasePort reconciliation) {
    this.reconciliation = reconciliation;
  }

  @Scheduled(fixedDelayString = "${resilience.payment.reconciliation.fixed-delay:10000}")
  public void reconcile() {
    reconciliation.reconcilePending();
  }
}
