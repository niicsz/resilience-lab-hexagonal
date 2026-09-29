package com.example.orders.domain;

public record TrackingInfo(String orderId, String carrier, String trackingCode, String state) {}
