package com.example.orders.config;

import com.example.orders.application.ports.inbound.OrderUseCasePort;
import com.example.orders.application.ports.inbound.PaymentReconciliationUseCasePort;
import com.example.orders.application.ports.outbound.*;
import com.example.orders.application.usecases.OrderUseCase;
import com.example.orders.application.usecases.PaymentReconciliationUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanConfiguration {
  @Bean
  OrderUseCasePort orderUseCase(
      OrderRepositoryPort orders,
      InventoryPort inventory,
      PaymentGatewayPort payments,
      NotificationPort notifications,
      TrackingPort tracking) {
    return new OrderUseCase(orders, inventory, payments, notifications, tracking);
  }

  @Bean
  PaymentReconciliationUseCasePort paymentReconciliationUseCase(
      OrderRepositoryPort orders, PaymentGatewayPort payments) {
    return new PaymentReconciliationUseCase(orders, payments);
  }
}
