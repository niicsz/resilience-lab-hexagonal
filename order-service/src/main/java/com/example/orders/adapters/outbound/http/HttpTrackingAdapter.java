package com.example.orders.adapters.outbound.http;

import com.example.orders.adapters.outbound.http.TrackingClient.TrackingResponse;
import com.example.orders.adapters.outbound.resilience.ResilienceFacade;
import com.example.orders.application.ports.outbound.TrackingPort;
import com.example.orders.domain.TrackingInfo;
import com.example.orders.domain.exception.DependencyOverloadedException;
import com.example.orders.domain.exception.ExternalServiceUnavailableException;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import java.util.concurrent.CompletionException;
import org.springframework.stereotype.Component;

@Component
public class HttpTrackingAdapter implements TrackingPort {
  private final TrackingClient client;
  private final ResilienceFacade resilience;

  public HttpTrackingAdapter(TrackingClient client, ResilienceFacade resilience) {
    this.client = client;
    this.resilience = resilience;
  }

  @Override
  public TrackingInfo track(String orderPublicId) {
    try {
      TrackingResponse response = resilience.tracking(() -> client.track(orderPublicId));
      return new TrackingInfo(
          response.orderPublicId(), response.carrier(), response.trackingCode(), response.state());
    } catch (BulkheadFullException exception) {
      throw new DependencyOverloadedException("tracking", exception);
    } catch (RuntimeException exception) {
      Throwable cause =
          exception instanceof CompletionException && exception.getCause() != null
              ? exception.getCause()
              : exception;
      throw new ExternalServiceUnavailableException("tracking unavailable", cause);
    }
  }
}
