package com.example.orders.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "clients")
public record ClientsProperties(
    Client payment, Client inventory, Client notification, Client tracking) {

  public record Client(String baseUrl) {}
}
