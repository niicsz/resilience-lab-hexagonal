package com.example.orders.adapters.outbound.http;

import com.example.orders.adapters.outbound.http.NotificationClient.NotificationRequest;
import com.example.orders.adapters.outbound.resilience.ResilienceFacade;
import com.example.orders.application.ports.outbound.NotificationPort;
import com.example.orders.domain.Order;
import com.example.orders.domain.exception.ExternalServiceUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class HttpNotificationAdapter implements NotificationPort {
  private static final Logger LOGGER = LoggerFactory.getLogger(HttpNotificationAdapter.class);
  private final NotificationClient client;
  private final ResilienceFacade resilience;

  public HttpNotificationAdapter(NotificationClient client, ResilienceFacade resilience) {
    this.client = client;
    this.resilience = resilience;
  }

  @Override
  public void orderUpdated(Order order) {
    String publicId = order.getPublicId();
    try {
      resilience.notification(
          () ->
              client.notify(
                  new NotificationRequest(publicId, "EMAIL", "Order " + publicId + " update")),
          () -> LOGGER.info("notificação adiada para o pedido {}", publicId));
    } catch (RuntimeException exception) {
      throw new ExternalServiceUnavailableException("notification unavailable", exception);
    }
  }
}
