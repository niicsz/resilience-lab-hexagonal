package com.example.orders.application.ports.inbound;

import com.example.orders.domain.Order;
import com.example.orders.domain.TrackingInfo;

public interface OrderUseCasePort {
  Order create(CreateOrderCommand command);

  Order get(String publicId);

  TrackingInfo track(String publicId);
}
