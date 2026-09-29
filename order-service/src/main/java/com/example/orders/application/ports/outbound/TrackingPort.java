package com.example.orders.application.ports.outbound;

import com.example.orders.domain.TrackingInfo;

public interface TrackingPort {
  TrackingInfo track(String orderPublicId);
}
