package com.example.orders.adapters.outbound.http;

import com.example.orders.adapters.outbound.http.InventoryClient.ReservationRequest;
import com.example.orders.adapters.outbound.http.InventoryClient.ReservationResponse;
import com.example.orders.adapters.outbound.resilience.ResilienceFacade;
import com.example.orders.application.ports.outbound.InventoryPort;
import com.example.orders.domain.Order;
import com.example.orders.domain.exception.ExternalServiceUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class HttpInventoryAdapter implements InventoryPort {
  private static final Logger LOGGER = LoggerFactory.getLogger(HttpInventoryAdapter.class);
  private final InventoryClient client;
  private final ResilienceFacade resilience;

  public HttpInventoryAdapter(InventoryClient client, ResilienceFacade resilience) {
    this.client = client;
    this.resilience = resilience;
  }

  @Override
  public boolean reserve(Order order) {
    try {
      ReservationResponse response =
          resilience.inventory(
              () ->
                  client.reserve(
                      new ReservationRequest(
                          order.getSku(), order.getQuantity(), order.getPublicId())));
      return response != null && response.reserved();
    } catch (RuntimeException exception) {
      LOGGER.warn(
          "inventory reservation failed after retries -> OUT_OF_STOCK: {}", exception.toString());
      throw new ExternalServiceUnavailableException("inventory unavailable", exception);
    }
  }
}
