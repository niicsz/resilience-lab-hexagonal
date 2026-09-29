package com.example.orders.application.ports.outbound;

import com.example.orders.domain.Order;
import com.example.orders.domain.PaymentOutcome;

public interface PaymentGatewayPort {
  PaymentOutcome charge(Order order);
}
