package com.example.orders.config;

import com.example.orders.adapters.outbound.http.InventoryClient;
import com.example.orders.adapters.outbound.http.NotificationClient;
import com.example.orders.adapters.outbound.http.PaymentClient;
import com.example.orders.adapters.outbound.http.TrackingClient;
import java.net.http.HttpClient;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.support.RestClientHttpServiceGroupConfigurer;
import org.springframework.web.service.registry.ImportHttpServices;

@Configuration(proxyBeanMethods = false)
@ImportHttpServices(group = "payment", types = PaymentClient.class)
@ImportHttpServices(group = "inventory", types = InventoryClient.class)
@ImportHttpServices(group = "notification", types = NotificationClient.class)
@ImportHttpServices(group = "tracking", types = TrackingClient.class)
public class HttpClientsConfig {

  @Bean
  RestClientHttpServiceGroupConfigurer clientsGroupConfigurer(ClientsProperties properties) {
    HttpClient httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
    JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);

    Map<String, String> baseUrls =
        Map.of(
            "payment", properties.payment().baseUrl(),
            "inventory", properties.inventory().baseUrl(),
            "notification", properties.notification().baseUrl(),
            "tracking", properties.tracking().baseUrl());

    return groups ->
        groups.forEachClient(
            (group, builder) -> {
              builder.requestFactory(requestFactory);
              String baseUrl = baseUrls.get(group.name());
              if (baseUrl != null) builder.baseUrl(baseUrl);
            });
  }
}
