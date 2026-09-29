package com.example.orders.adapters.outbound.resilience;

import static org.assertj.core.api.Assertions.*;

import com.example.orders.config.ResilienceConfig;
import com.example.orders.config.ResilienceProperties;
import com.example.orders.domain.exception.PaymentDeclinedException;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpServerErrorException;

class ResilienceFacadeTest {
  private ResilienceProperties props;

  @BeforeEach
  void setUp() {
    props = new ResilienceProperties();
    props.getPayment().getRetry().setWaitDuration(Duration.ofMillis(10));
    props.getInventory().getRetry().setWaitDuration(Duration.ofMillis(10));
  }

  private ResilienceFacade facade() {
    ResilienceConfig config = new ResilienceConfig(props);
    return new ResilienceFacade(
        props,
        config.circuitBreakerRegistry(),
        config.retryRegistry(),
        config.timeLimiterRegistry(),
        config.threadPoolBulkheadRegistry(),
        config.paymentExecutor());
  }

  private static HttpServerErrorException serverError() {
    return new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE);
  }

  @Test
  void paymentTimeoutAbortsSlowCall() {
    props.getPayment().getTimeout().setDuration(Duration.ofMillis(100));
    props.getPayment().getCircuitbreaker().setEnabled(false);
    props.getPayment().getRetry().setEnabled(false);

    assertThatThrownBy(
            () ->
                facade()
                    .payment(
                        () -> {
                          sleep(500);
                          return "ok";
                        }))
        .isInstanceOf(TimeoutException.class);
  }

  @Test
  void paymentRetriesTransientFailures() throws Exception {
    props.getPayment().getCircuitbreaker().setEnabled(false);
    AtomicInteger calls = new AtomicInteger();

    String result =
        facade()
            .payment(
                () -> {
                  if (calls.incrementAndGet() < 3) throw serverError();
                  return "approved";
                });

    assertThat(result).isEqualTo("approved");
    assertThat(calls).hasValue(3);
  }

  @Test
  void paymentDeclineIsNeverRetried() {
    AtomicInteger calls = new AtomicInteger();

    assertThatThrownBy(
            () ->
                facade()
                    .payment(
                        () -> {
                          calls.incrementAndGet();
                          throw new PaymentDeclinedException("declined");
                        }))
        .isInstanceOf(PaymentDeclinedException.class);
    assertThat(calls).hasValue(1);
  }

  @Test
  void openCircuitFailsFastWithoutCallingPayment() {
    props.getPayment().getCircuitbreaker().setSlidingWindowSize(4);
    props.getPayment().getRetry().setEnabled(false);
    props.getPayment().getTimeout().setEnabled(false);
    ResilienceFacade facade = facade();
    AtomicInteger calls = new AtomicInteger();

    for (int i = 0; i < 4; i++) {
      assertThatThrownBy(
              () ->
                  facade.payment(
                      () -> {
                        calls.incrementAndGet();
                        throw serverError();
                      }))
          .isInstanceOf(HttpServerErrorException.class);
    }

    assertThatThrownBy(() -> facade.payment(calls::incrementAndGet))
        .isInstanceOf(CallNotPermittedException.class);
    assertThat(calls).hasValue(4);
  }

  @Test
  void inventoryRetriesThenSucceeds() {
    AtomicInteger calls = new AtomicInteger();

    String result =
        facade()
            .inventory(
                () -> {
                  if (calls.incrementAndGet() < 3) throw serverError();
                  return "reserved";
                });

    assertThat(result).isEqualTo("reserved");
    assertThat(calls).hasValue(3);
  }

  @Test
  void inventoryBypassesRetryWhenDisabled() {
    props.getInventory().getRetry().setEnabled(false);
    AtomicInteger calls = new AtomicInteger();

    assertThatThrownBy(
            () ->
                facade()
                    .inventory(
                        () -> {
                          calls.incrementAndGet();
                          throw serverError();
                        }))
        .isInstanceOf(HttpServerErrorException.class);
    assertThat(calls).hasValue(1);
  }

  @Test
  void trackingBulkheadRejectsWhenSaturated() throws InterruptedException {
    props.getTracking().getBulkhead().setCoreSize(1);
    props.getTracking().getBulkhead().setMaxSize(1);
    props.getTracking().getBulkhead().setQueueCapacity(1);
    ResilienceFacade facade = facade();
    CountDownLatch hold = new CountDownLatch(1);
    CountDownLatch started = new CountDownLatch(1);

    Thread.startVirtualThread(
        () ->
            facade.tracking(
                () -> {
                  started.countDown();
                  await(hold);
                  return "busy";
                }));
    started.await();
    Thread.startVirtualThread(
        () ->
            facade.tracking(
                () -> {
                  await(hold);
                  return "queued";
                }));
    sleep(100);

    assertThatThrownBy(() -> facade.tracking(() -> "rejected"))
        .isInstanceOf(BulkheadFullException.class);
    hold.countDown();
  }

  @Test
  void notificationFallbackSwallowsFailure() {
    AtomicInteger fallback = new AtomicInteger();

    facade()
        .notification(
            () -> {
              throw new RuntimeException("down");
            },
            fallback::incrementAndGet);

    assertThat(fallback).hasValue(1);
  }

  @Test
  void notificationFailurePropagatesWhenFallbackDisabled() {
    props.getNotification().getFallback().setEnabled(false);

    assertThatThrownBy(
            () ->
                facade()
                    .notification(
                        () -> {
                          throw new RuntimeException("down");
                        },
                        () -> {}))
        .hasMessage("down");
  }

  private static void sleep(long ms) {
    try {
      Thread.sleep(ms);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  private static void await(CountDownLatch latch) {
    try {
      latch.await();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
