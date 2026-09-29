package com.example.orders.adapters.inbound.web;

import com.example.orders.adapters.inbound.web.dto.CreateOrderRequest;
import com.example.orders.adapters.inbound.web.dto.OrderResponse;
import com.example.orders.adapters.inbound.web.dto.TrackingView;
import com.example.orders.application.ports.inbound.OrderUseCasePort;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
public class OrderController {
  private final OrderUseCasePort orders;

  public OrderController(OrderUseCasePort orders) {
    this.orders = orders;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public OrderResponse create(@Valid @RequestBody CreateOrderRequest request) {
    return OrderResponse.from(orders.create(request.toCommand()));
  }

  @GetMapping("/{publicId}")
  public OrderResponse get(@PathVariable String publicId) {
    return OrderResponse.from(orders.get(publicId));
  }

  @GetMapping("/{publicId}/tracking")
  public TrackingView tracking(@PathVariable String publicId) {
    return TrackingView.from(orders.track(publicId));
  }
}
