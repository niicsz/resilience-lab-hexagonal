package com.example.orders.adapters.outbound.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.orders.domain.Order;
import com.example.orders.domain.OrderStatus;
import com.example.orders.domain.PaymentOutcome;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@DataJpaTest
@Import(OrderRepositoryAdapter.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class OrderRepositoryAdapterTest {
  @Container @ServiceConnection static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

  @Autowired OrderRepositoryAdapter adapter;

  private Order place(String sku) {
    return Order.place(sku, 2, new BigDecimal("19.90"), "BRL");
  }

  @Test
  void savesNewOrderAndFindsByPublicId() {
    Order saved = adapter.save(place("SKU-1"));

    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getCreatedAt()).isNotNull();
    assertThat(adapter.findByPublicId(saved.getPublicId()))
        .get()
        .satisfies(
            found -> {
              assertThat(found.getStatus()).isEqualTo(OrderStatus.PENDING);
              assertThat(found.getAmount()).isEqualByComparingTo("19.90");
            });
  }

  @Test
  void updatesStatusKeepingIdentityAndCreationTime() {
    Order saved = adapter.save(place("SKU-2"));
    saved.applyPayment(PaymentOutcome.PENDING);

    Order updated = adapter.save(saved);

    assertThat(updated.getId()).isEqualTo(saved.getId());
    assertThat(updated.getCreatedAt()).isEqualTo(saved.getCreatedAt());
    assertThat(adapter.findByStatus(OrderStatus.PAYMENT_PENDING))
        .extracting(Order::getPublicId)
        .contains(saved.getPublicId());
  }
}
