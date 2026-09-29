package com.example.orders.adapters.outbound.http;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange
public interface InventoryClient {
  @PostExchange("/inventory/reservations")
  ReservationResponse reserve(@RequestBody ReservationRequest request);

  record ReservationRequest(String sku, int quantity, String orderPublicId) {}

  record ReservationResponse(boolean reserved) {}
}
