package com.example.orders.config;

import com.example.orders.domain.exception.PaymentDeclinedException;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadConfig;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.micrometer.tagged.TaggedCircuitBreakerMetrics;
import io.github.resilience4j.micrometer.tagged.TaggedRetryMetrics;
import io.github.resilience4j.micrometer.tagged.TaggedThreadPoolBulkheadMetrics;
import io.github.resilience4j.micrometer.tagged.TaggedTimeLimiterMetrics;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

@Configuration
public class ResilienceConfig {
  private final ResilienceProperties props;

  public ResilienceConfig(ResilienceProperties props) {
    this.props = props;
  }

  @Bean
  public CircuitBreakerRegistry circuitBreakerRegistry() {
    ResilienceProperties.CircuitBreaker cb = props.getPayment().getCircuitbreaker();
    CircuitBreakerConfig config =
        CircuitBreakerConfig.custom()
            .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
            .slidingWindowSize(cb.getSlidingWindowSize())
            .failureRateThreshold(cb.getFailureRateThreshold())
            .waitDurationInOpenState(cb.getWaitDurationOpen())
            .permittedNumberOfCallsInHalfOpenState(cb.getPermittedCallsHalfOpen())
            .build();

    CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
    registry.circuitBreaker("payment", config);
    return registry;
  }

  @Bean
  public RetryRegistry retryRegistry() {
    RetryRegistry registry = RetryRegistry.ofDefaults();

    ResilienceProperties.Retry payment = props.getPayment().getRetry();
    registry.retry(
        "payment",
        RetryConfig.custom()
            .maxAttempts(payment.getMaxAttempts())
            .intervalFunction(backoff(payment))
            .ignoreExceptions(CallNotPermittedException.class, PaymentDeclinedException.class)
            .build());

    ResilienceProperties.Retry inventory = props.getInventory().getRetry();
    registry.retry(
        "inventory",
        RetryConfig.custom()
            .maxAttempts(inventory.getMaxAttempts())
            .intervalFunction(backoff(inventory))
            .retryExceptions(
                HttpServerErrorException.class, IOException.class, ResourceAccessException.class)
            .build());

    return registry;
  }

  @Bean
  public TimeLimiterRegistry timeLimiterRegistry() {
    TimeLimiterRegistry registry = TimeLimiterRegistry.ofDefaults();
    registry.timeLimiter(
        "payment",
        TimeLimiterConfig.custom()
            .timeoutDuration(props.getPayment().getTimeout().getDuration())
            .cancelRunningFuture(true)
            .build());
    return registry;
  }

  @Bean
  public ThreadPoolBulkheadRegistry threadPoolBulkheadRegistry() {
    ResilienceProperties.Bulkhead bh = props.getTracking().getBulkhead();
    ThreadPoolBulkheadConfig config =
        ThreadPoolBulkheadConfig.custom()
            .coreThreadPoolSize(bh.getCoreSize())
            .maxThreadPoolSize(bh.getMaxSize())
            .queueCapacity(bh.getQueueCapacity())
            .build();

    ThreadPoolBulkheadRegistry registry = ThreadPoolBulkheadRegistry.ofDefaults();
    registry.bulkhead("tracking", config);
    return registry;
  }

  @Bean
  public ExecutorService paymentExecutor() {
    return Executors.newCachedThreadPool();
  }

  @Bean
  public TaggedCircuitBreakerMetrics circuitBreakerMetrics(
      CircuitBreakerRegistry registry, MeterRegistry meterRegistry) {
    TaggedCircuitBreakerMetrics metrics =
        TaggedCircuitBreakerMetrics.ofCircuitBreakerRegistry(registry);
    metrics.bindTo(meterRegistry);
    return metrics;
  }

  @Bean
  public TaggedRetryMetrics retryMetrics(RetryRegistry registry, MeterRegistry meterRegistry) {
    TaggedRetryMetrics metrics = TaggedRetryMetrics.ofRetryRegistry(registry);
    metrics.bindTo(meterRegistry);
    return metrics;
  }

  @Bean
  public TaggedTimeLimiterMetrics timeLimiterMetrics(
      TimeLimiterRegistry registry, MeterRegistry meterRegistry) {
    TaggedTimeLimiterMetrics metrics = TaggedTimeLimiterMetrics.ofTimeLimiterRegistry(registry);
    metrics.bindTo(meterRegistry);
    return metrics;
  }

  @Bean
  public TaggedThreadPoolBulkheadMetrics bulkheadMetrics(
      ThreadPoolBulkheadRegistry registry, MeterRegistry meterRegistry) {
    TaggedThreadPoolBulkheadMetrics metrics =
        TaggedThreadPoolBulkheadMetrics.ofThreadPoolBulkheadRegistry(registry);
    metrics.bindTo(meterRegistry);
    return metrics;
  }

  private static IntervalFunction backoff(ResilienceProperties.Retry retry) {
    return IntervalFunction.ofExponentialRandomBackoff(
        retry.getWaitDuration(), retry.getMultiplier(), retry.getJitter());
  }
}
