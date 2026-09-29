package com.example.orders.adapters.inbound.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.orders.application.ports.inbound.CreateOrderCommand;
import com.example.orders.application.ports.inbound.OrderUseCasePort;
import com.example.orders.domain.Order;
import com.example.orders.domain.OrderStatus;
import com.example.orders.domain.TrackingInfo;
import com.example.orders.domain.exception.DependencyOverloadedException;
import com.example.orders.domain.exception.ExternalServiceUnavailableException;
import com.example.orders.domain.exception.OrderNotFoundException;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OrderController.class)
class OrderControllerTest {
  @Autowired MockMvc mvc;

  @MockitoBean OrderUseCasePort orders;

  private Order confirmed() {
    return Order.restore(
        1L,
        "ORD-1",
        "SKU-1",
        2,
        new BigDecimal("19.90"),
        "BRL",
        OrderStatus.CONFIRMED,
        Instant.now(),
        Instant.now());
  }

  @Test
  void createReturns201WithStatus() throws Exception {
    when(orders.create(any(CreateOrderCommand.class))).thenReturn(confirmed());

    mvc.perform(
            post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"sku":"SKU-1","quantity":2,"amount":19.90,"currency":"BRL"}
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.orderId").value("ORD-1"))
        .andExpect(jsonPath("$.status").value("CONFIRMED"))
        .andExpect(jsonPath("$.amount").value(19.90));
  }

  @Test
  void createRejectsInvalidBodyWith400() throws Exception {
    mvc.perform(
            post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"sku":"","quantity":0,"amount":-1,"currency":""}
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("validation_error"));
  }

  @Test
  void createRejectsMalformedJsonWith400() throws Exception {
    mvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON).content("{not json"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("validation_error"));
  }

  @Test
  void getReturnsOrder() throws Exception {
    when(orders.get("ORD-1")).thenReturn(confirmed());

    mvc.perform(get("/orders/{id}", "ORD-1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CONFIRMED"));
  }

  @Test
  void getReturns404WhenMissing() throws Exception {
    when(orders.get("missing")).thenThrow(new OrderNotFoundException("missing"));

    mvc.perform(get("/orders/{id}", "missing"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("order_not_found"));
  }

  @Test
  void trackingReturns200() throws Exception {
    when(orders.track("ORD-1")).thenReturn(new TrackingInfo("ORD-1", "ACME", "XYZ", "IN_TRANSIT"));

    mvc.perform(get("/orders/{id}/tracking", "ORD-1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.orderId").value("ORD-1"))
        .andExpect(jsonPath("$.state").value("IN_TRANSIT"));
  }

  @Test
  void trackingReturns503WhenOverloaded() throws Exception {
    when(orders.track("ORD-1")).thenThrow(new DependencyOverloadedException("tracking", null));

    mvc.perform(get("/orders/{id}/tracking", "ORD-1"))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.error").value("tracking_overloaded"));
  }

  @Test
  void dependencyFailureReturns503() throws Exception {
    when(orders.track("ORD-1"))
        .thenThrow(new ExternalServiceUnavailableException("tracking unavailable", null));

    mvc.perform(get("/orders/{id}/tracking", "ORD-1"))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.error").value("dependency_unavailable"));
  }

  @Test
  void unknownRouteReturns404InsteadOf500() throws Exception {
    mvc.perform(get("/nope")).andExpect(status().isNotFound());
  }
}
