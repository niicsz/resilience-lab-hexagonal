package com.example.orders.adapters.outbound.resilience;

import com.example.orders.config.ResilienceProperties;
import io.github.resilience4j.bulkhead.ThreadPoolBulkhead;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ResilienceFacade {
  private static final Logger LOGGER = LoggerFactory.getLogger(ResilienceFacade.class);
  private final ResilienceProperties props;
  private final CircuitBreakerRegistry circuitBreakerRegistry;
  private final RetryRegistry retryRegistry;
  private final TimeLimiterRegistry timeLimiterRegistry;
  private final ThreadPoolBulkheadRegistry threadPoolBulkheadRegistry;
  private final ExecutorService paymentExecutor;

  public ResilienceFacade(
      ResilienceProperties props,
      CircuitBreakerRegistry circuitBreakerRegistry,
      RetryRegistry retryRegistry,
      TimeLimiterRegistry timeLimiterRegistry,
      ThreadPoolBulkheadRegistry threadPoolBulkheadRegistry,
      ExecutorService paymentExecutor) {
    this.props = props;
    this.circuitBreakerRegistry = circuitBreakerRegistry;
    this.retryRegistry = retryRegistry;
    this.timeLimiterRegistry = timeLimiterRegistry;
    this.threadPoolBulkheadRegistry = threadPoolBulkheadRegistry;
    this.paymentExecutor = paymentExecutor;
  }

  public <T> T payment(Supplier<T> call) throws Exception {
    Callable<T> callable;
    if (props.getPayment().getTimeout().isEnabled()) {
      TimeLimiter timeLimiter = timeLimiterRegistry.timeLimiter("payment");
      callable =
          TimeLimiter.decorateFutureSupplier(
              timeLimiter, () -> CompletableFuture.supplyAsync(call, paymentExecutor));
    } else {
      callable = call::get;
    }
    if (props.getPayment().getCircuitbreaker().isEnabled()) {
      CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("payment");
      callable = CircuitBreaker.decorateCallable(circuitBreaker, callable);
    }
    if (props.getPayment().getRetry().isEnabled()) {
      Retry retry = retryRegistry.retry("payment");
      callable = Retry.decorateCallable(retry, callable);
    }
    return callable.call();
  }

  public <T> T inventory(Supplier<T> call) {
    if (!props.getInventory().getRetry().isEnabled()) return call.get();
    return Retry.decorateSupplier(retryRegistry.retry("inventory"), call).get();
  }

  public <T> T tracking(Supplier<T> call) {
    if (!props.getTracking().getBulkhead().isEnabled()) return call.get();
    ThreadPoolBulkhead bulkhead = threadPoolBulkheadRegistry.bulkhead("tracking");
    return ThreadPoolBulkhead.decorateSupplier(bulkhead, call).get().toCompletableFuture().join();
  }

  public void notification(Runnable call, Runnable fallback) {
    if (!props.getNotification().getFallback().isEnabled()) {
      call.run();
      return;
    }
    try {
      call.run();
    } catch (Exception exception) {
      LOGGER.warn("notification failed -> notificação adiada: {}", exception.toString());
      fallback.run();
    }
  }
}
