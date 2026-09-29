package com.example.orders.adapters.outbound.http;

import com.example.orders.adapters.outbound.http.PaymentClient.PaymentRequest;
import com.example.orders.adapters.outbound.http.PaymentClient.PaymentResponse;
import com.example.orders.adapters.outbound.resilience.ResilienceFacade;
import com.example.orders.application.ports.outbound.PaymentGatewayPort;
import com.example.orders.domain.Order;
import com.example.orders.domain.PaymentOutcome;
import com.example.orders.domain.exception.ExternalServiceUnavailableException;
import com.example.orders.domain.exception.PaymentDeclinedException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

@Component
public class HttpPaymentAdapter implements PaymentGatewayPort {
  private static final Logger LOGGER = LoggerFactory.getLogger(HttpPaymentAdapter.class);
  private final PaymentClient client;
  private final ResilienceFacade resilience;

  public HttpPaymentAdapter(PaymentClient client, ResilienceFacade resilience) {
    this.client = client;
    this.resilience = resilience;
  }

  @Override
  public PaymentOutcome charge(Order order) {
    try {
      return toOutcome(resilience.payment(() -> call(order)));
    } catch (PaymentDeclinedException exception) {
      return PaymentOutcome.DECLINED;
    } catch (CallNotPermittedException exception) {
      LOGGER.warn("payment circuit open -> fallback (PAYMENT_PENDING)");
      throw new ExternalServiceUnavailableException("payment circuit open", exception);
    } catch (TimeoutException exception) {
      LOGGER.warn("payment timed out -> fallback (PAYMENT_PENDING)");
      throw new ExternalServiceUnavailableException("payment timed out", exception);
    } catch (Exception exception) {
      LOGGER.warn(
          "payment transient failure -> fallback (PAYMENT_PENDING): {}", exception.toString());
      throw new ExternalServiceUnavailableException("payment unavailable", exception);
    }
  }

  private PaymentResponse call(Order order) {
    try {
      return client.charge(
          new PaymentRequest(order.getPublicId(), order.getAmount(), order.getCurrency()));
    } catch (HttpClientErrorException exception) {
      if (exception.getStatusCode().isSameCodeAs(HttpStatus.PAYMENT_REQUIRED))
        throw new PaymentDeclinedException("payment declined for order " + order.getPublicId());
      throw exception;
    }
  }

  private static PaymentOutcome toOutcome(PaymentResponse response) {
    if (response == null || response.status() == null) return PaymentOutcome.PENDING;
    return switch (response.status()) {
      case "APPROVED" -> PaymentOutcome.APPROVED;
      case "DECLINED" -> PaymentOutcome.DECLINED;
      default -> PaymentOutcome.PENDING;
    };
  }
}
