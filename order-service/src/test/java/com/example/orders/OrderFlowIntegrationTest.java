package com.example.orders;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderFlowIntegrationTest {
  private static final String ORDER =
      "{\"sku\":\"SKU-1\",\"quantity\":2,\"amount\":19.90,\"currency\":\"BRL\"}";

  @Container @ServiceConnection static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

  static WireMockServer wiremock;

  @BeforeAll
  static void startWiremock() {
    wiremock = new WireMockServer(options().dynamicPort());
    wiremock.start();
  }

  @AfterAll
  static void stopWiremock() {
    wiremock.stop();
  }

  @DynamicPropertySource
  static void props(DynamicPropertyRegistry r) {
    String base = "http://localhost:" + wiremock.port();
    r.add("clients.payment.base-url", () -> base);
    r.add("clients.inventory.base-url", () -> base);
    r.add("clients.notification.base-url", () -> base);
    r.add("clients.tracking.base-url", () -> base);
  }

  @LocalServerPort int port;

  @BeforeEach
  void stubHappyPath() {
    wiremock.resetAll();
    wiremock.stubFor(post("/inventory/reservations").willReturn(okJson("{\"reserved\":true}")));
    wiremock.stubFor(post("/payments").willReturn(okJson("{\"status\":\"APPROVED\"}")));
    wiremock.stubFor(post("/notifications").willReturn(aResponse().withStatus(202)));
  }

  private RestClient app() {
    return RestClient.builder().baseUrl("http://localhost:" + port).build();
  }

  private String createOrder() {
    return app()
        .post()
        .uri("/orders")
        .contentType(MediaType.APPLICATION_JSON)
        .body(ORDER)
        .retrieve()
        .body(String.class);
  }

  @Test
  void postOrderConfirmsEndToEndAndCanBeRead() {
    String created = createOrder();
    assertThat(created).contains("\"status\":\"CONFIRMED\"");

    String orderId = created.replaceAll(".*\"orderId\":\"([^\"]+)\".*", "$1");
    String read = app().get().uri("/orders/{id}", orderId).retrieve().body(String.class);

    assertThat(read).contains("\"orderId\":\"" + orderId + "\"").contains("CONFIRMED");
  }

  @Test
  void paymentOutageFallsBackToPaymentPending() {
    wiremock.stubFor(post("/payments").willReturn(aResponse().withStatus(500)));

    assertThat(createOrder()).contains("\"status\":\"PAYMENT_PENDING\"");
  }

  @Test
  void prometheusEndpointExposesResilienceMetrics() {
    String metrics = app().get().uri("/actuator/prometheus").retrieve().body(String.class);

    assertThat(metrics)
        .contains("resilience4j_circuitbreaker_state")
        .contains("resilience4j_retry_calls")
        .contains("resilience4j_bulkhead")
        .contains("http_server_requests");
  }
}
