package com.example.orders.adapters.outbound.http;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange
public interface NotificationClient {
  @PostExchange("/notifications")
  void notify(@RequestBody NotificationRequest request);

  record NotificationRequest(String orderPublicId, String channel, String message) {}
}
