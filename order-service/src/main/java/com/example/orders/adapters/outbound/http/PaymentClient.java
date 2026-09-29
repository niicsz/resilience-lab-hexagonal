package com.example.orders.adapters.outbound.http;

import java.math.BigDecimal;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange
public interface PaymentClient {
  @PostExchange("/payments")
  PaymentResponse charge(@RequestBody PaymentRequest request);

  record PaymentRequest(String orderPublicId, BigDecimal amount, String currency) {}

  record PaymentResponse(String status) {}
}
