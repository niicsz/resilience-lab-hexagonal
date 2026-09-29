package com.example.orders.adapters.inbound.web.dto;

import com.example.orders.domain.TrackingInfo;

public record TrackingView(String orderId, String carrier, String trackingCode, String state) {

  public static TrackingView from(TrackingInfo tracking) {
    return new TrackingView(
        tracking.orderId(), tracking.carrier(), tracking.trackingCode(), tracking.state());
  }
}
