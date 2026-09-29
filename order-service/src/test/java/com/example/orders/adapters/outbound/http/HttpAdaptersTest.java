package com.example.orders.adapters.outbound.http;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.orders.adapters.outbound.resilience.ResilienceFacade;
import com.example.orders.config.ResilienceConfig;
import com.example.orders.config.ResilienceProperties;
import com.example.orders.domain.Order;
import com.example.orders.domain.PaymentOutcome;
import com.example.orders.domain.exception.ExternalServiceUnavailableException;
import com.github.tomakehurst.wiremock.WireMockServer;
import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.Duration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

class HttpAdaptersTest {
  static WireMockServer server;
  private final Order order = Order.place("SKU-1", 2, new BigDecimal("19.90"), "BRL");
  private ResilienceProperties props;

  @BeforeAll
  static void start() {
    server = new WireMockServer(options().dynamicPort());
    server.start();
  }

  @AfterAll
  static void stop() {
    server.stop();
  }

  @BeforeEach
  void setUp() {
    server.resetAll();
    props = new ResilienceProperties();
    props.getPayment().getRetry().setWaitDuration(Duration.ofMillis(10));
    props.getInventory().getRetry().setWaitDuration(Duration.ofMillis(10));
  }

  private <T> T client(Class<T> type) {
    RestClient rest =
        RestClient.builder()
            .baseUrl(server.baseUrl())
            .requestFactory(
                new JdkClientHttpRequestFactory(
                    HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build()))
            .build();
    return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(rest))
        .build()
        .createClient(type);
  }

  private ResilienceFacade resilience() {
    ResilienceConfig config = new ResilienceConfig(props);
    return new ResilienceFacade(
        props,
        config.circuitBreakerRegistry(),
        config.retryRegistry(),
        config.timeLimiterRegistry(),
        config.threadPoolBulkheadRegistry(),
        config.paymentExecutor());
  }

  private HttpPaymentAdapter payment() {
    return new HttpPaymentAdapter(client(PaymentClient.class), resilience());
  }

  @Test
  void approvedPayment() {
    server.stubFor(post("/payments").willReturn(okJson("{\"status\":\"APPROVED\"}")));

    assertThat(payment().charge(order)).isEqualTo(PaymentOutcome.APPROVED);
    server.verify(
        postRequestedFor(urlEqualTo("/payments"))
            .withRequestBody(matchingJsonPath("$.orderPublicId", equalTo(order.getPublicId())))
            .withRequestBody(matchingJsonPath("$.currency", equalTo("BRL"))));
  }

  @Test
  void declinedPaymentBody() {
    server.stubFor(post("/payments").willReturn(okJson("{\"status\":\"DECLINED\"}")));

    assertThat(payment().charge(order)).isEqualTo(PaymentOutcome.DECLINED);
  }

  @Test
  void paymentRequiredIsDeclinedWithoutRetry() {
    server.stubFor(post("/payments").willReturn(aResponse().withStatus(402)));

    assertThat(payment().charge(order)).isEqualTo(PaymentOutcome.DECLINED);
    server.verify(exactly(1), postRequestedFor(urlEqualTo("/payments")));
  }

  @Test
  void paymentServerErrorIsUnavailableAfterRetries() {
    server.stubFor(post("/payments").willReturn(aResponse().withStatus(500)));

    assertThatThrownBy(() -> payment().charge(order))
        .isInstanceOf(ExternalServiceUnavailableException.class);
    server.verify(exactly(3), postRequestedFor(urlEqualTo("/payments")));
  }

  @Test
  void slowPaymentTimesOut() {
    props.getPayment().getTimeout().setDuration(Duration.ofMillis(100));
    props.getPayment().getRetry().setEnabled(false);
    server.stubFor(
        post("/payments").willReturn(okJson("{\"status\":\"APPROVED\"}").withFixedDelay(1_000)));

    assertThatThrownBy(() -> payment().charge(order))
        .isInstanceOf(ExternalServiceUnavailableException.class)
        .hasMessage("payment timed out");
  }

  @Test
  void inventoryReservation() {
    server.stubFor(post("/inventory/reservations").willReturn(okJson("{\"reserved\":true}")));

    assertThat(new HttpInventoryAdapter(client(InventoryClient.class), resilience()).reserve(order))
        .isTrue();
    server.verify(
        postRequestedFor(urlEqualTo("/inventory/reservations"))
            .withRequestBody(matchingJsonPath("$.sku", equalTo("SKU-1")))
            .withRequestBody(matchingJsonPath("$.quantity", equalTo("2"))));
  }

  @Test
  void inventoryUnavailableAfterRetries() {
    server.stubFor(post("/inventory/reservations").willReturn(aResponse().withStatus(503)));
    HttpInventoryAdapter adapter =
        new HttpInventoryAdapter(client(InventoryClient.class), resilience());

    assertThatThrownBy(() -> adapter.reserve(order))
        .isInstanceOf(ExternalServiceUnavailableException.class);
    server.verify(exactly(3), postRequestedFor(urlEqualTo("/inventory/reservations")));
  }

  @Test
  void trackingMapsResponse() {
    server.stubFor(
        get(urlPathMatching("/tracking/.*"))
            .willReturn(
                okJson(
                    "{\"orderPublicId\":\"ORD-1\",\"carrier\":\"ACME\","
                        + "\"trackingCode\":\"XYZ\",\"state\":\"IN_TRANSIT\"}")));

    var tracking =
        new HttpTrackingAdapter(client(TrackingClient.class), resilience()).track("ORD-1");

    assertThat(tracking.orderId()).isEqualTo("ORD-1");
    assertThat(tracking.state()).isEqualTo("IN_TRANSIT");
    server.verify(getRequestedFor(urlEqualTo("/tracking/ORD-1")));
  }

  @Test
  void trackingFailureIsUnavailable() {
    server.stubFor(get(urlPathMatching("/tracking/.*")).willReturn(aResponse().withStatus(500)));
    HttpTrackingAdapter adapter =
        new HttpTrackingAdapter(client(TrackingClient.class), resilience());

    assertThatThrownBy(() -> adapter.track("ORD-1"))
        .isInstanceOf(ExternalServiceUnavailableException.class);
  }

  @Test
  void notificationFailureFallsBack() {
    server.stubFor(post("/notifications").willReturn(aResponse().withStatus(500)));

    new HttpNotificationAdapter(client(NotificationClient.class), resilience()).orderUpdated(order);

    server.verify(postRequestedFor(urlEqualTo("/notifications")));
  }

  @Test
  void notificationFailurePropagatesWithoutFallback() {
    props.getNotification().getFallback().setEnabled(false);
    server.stubFor(post("/notifications").willReturn(aResponse().withStatus(500)));
    HttpNotificationAdapter adapter =
        new HttpNotificationAdapter(client(NotificationClient.class), resilience());

    assertThatThrownBy(() -> adapter.orderUpdated(order))
        .isInstanceOf(ExternalServiceUnavailableException.class);
  }
}
