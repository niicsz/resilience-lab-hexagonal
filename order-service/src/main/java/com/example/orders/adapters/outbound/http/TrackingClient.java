package com.example.orders.adapters.outbound.http;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange
public interface TrackingClient {
  @GetExchange("/tracking/{orderPublicId}")
  TrackingResponse track(@PathVariable String orderPublicId);

  record TrackingResponse(
      String orderPublicId, String carrier, String trackingCode, String state) {}
}
