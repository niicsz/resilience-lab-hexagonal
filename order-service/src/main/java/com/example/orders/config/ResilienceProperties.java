package com.example.orders.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "resilience")
public class ResilienceProperties {
  private Payment payment = new Payment();
  private Inventory inventory = new Inventory();
  private Tracking tracking = new Tracking();
  private Notification notification = new Notification();

  public Payment getPayment() {
    return payment;
  }

  public void setPayment(Payment payment) {
    this.payment = payment;
  }

  public Inventory getInventory() {
    return inventory;
  }

  public void setInventory(Inventory inventory) {
    this.inventory = inventory;
  }

  public Tracking getTracking() {
    return tracking;
  }

  public void setTracking(Tracking tracking) {
    this.tracking = tracking;
  }

  public Notification getNotification() {
    return notification;
  }

  public void setNotification(Notification notification) {
    this.notification = notification;
  }

  public static class Payment {
    private Timeout timeout = new Timeout();
    private CircuitBreaker circuitbreaker = new CircuitBreaker();
    private Retry retry = new Retry();

    public Timeout getTimeout() {
      return timeout;
    }

    public void setTimeout(Timeout timeout) {
      this.timeout = timeout;
    }

    public CircuitBreaker getCircuitbreaker() {
      return circuitbreaker;
    }

    public void setCircuitbreaker(CircuitBreaker circuitbreaker) {
      this.circuitbreaker = circuitbreaker;
    }

    public Retry getRetry() {
      return retry;
    }

    public void setRetry(Retry retry) {
      this.retry = retry;
    }
  }

  public static class Timeout {
    private boolean enabled = true;
    private Duration duration = Duration.ofMillis(800);

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public Duration getDuration() {
      return duration;
    }

    public void setDuration(Duration duration) {
      this.duration = duration;
    }
  }

  public static class CircuitBreaker {
    private boolean enabled = true;
    private int slidingWindowSize = 20;
    private float failureRateThreshold = 50f;
    private Duration waitDurationOpen = Duration.ofSeconds(10);
    private int permittedCallsHalfOpen = 3;

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public int getSlidingWindowSize() {
      return slidingWindowSize;
    }

    public void setSlidingWindowSize(int slidingWindowSize) {
      this.slidingWindowSize = slidingWindowSize;
    }

    public float getFailureRateThreshold() {
      return failureRateThreshold;
    }

    public void setFailureRateThreshold(float failureRateThreshold) {
      this.failureRateThreshold = failureRateThreshold;
    }

    public Duration getWaitDurationOpen() {
      return waitDurationOpen;
    }

    public void setWaitDurationOpen(Duration waitDurationOpen) {
      this.waitDurationOpen = waitDurationOpen;
    }

    public int getPermittedCallsHalfOpen() {
      return permittedCallsHalfOpen;
    }

    public void setPermittedCallsHalfOpen(int permittedCallsHalfOpen) {
      this.permittedCallsHalfOpen = permittedCallsHalfOpen;
    }
  }

  public static class Retry {
    private boolean enabled = true;
    private int maxAttempts = 3;
    private Duration waitDuration = Duration.ofMillis(200);
    private double multiplier = 2.0;
    private double jitter = 0.5;

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public int getMaxAttempts() {
      return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
      this.maxAttempts = maxAttempts;
    }

    public Duration getWaitDuration() {
      return waitDuration;
    }

    public void setWaitDuration(Duration waitDuration) {
      this.waitDuration = waitDuration;
    }

    public double getMultiplier() {
      return multiplier;
    }

    public void setMultiplier(double multiplier) {
      this.multiplier = multiplier;
    }

    public double getJitter() {
      return jitter;
    }

    public void setJitter(double jitter) {
      this.jitter = jitter;
    }
  }

  public static class Inventory {
    private Retry retry = new Retry();

    public Retry getRetry() {
      return retry;
    }

    public void setRetry(Retry retry) {
      this.retry = retry;
    }
  }

  public static class Tracking {
    private Bulkhead bulkhead = new Bulkhead();

    public Bulkhead getBulkhead() {
      return bulkhead;
    }

    public void setBulkhead(Bulkhead bulkhead) {
      this.bulkhead = bulkhead;
    }
  }

  public static class Bulkhead {
    private boolean enabled = true;
    private int coreSize = 4;
    private int maxSize = 4;
    private int queueCapacity = 2;

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public int getCoreSize() {
      return coreSize;
    }

    public void setCoreSize(int coreSize) {
      this.coreSize = coreSize;
    }

    public int getMaxSize() {
      return maxSize;
    }

    public void setMaxSize(int maxSize) {
      this.maxSize = maxSize;
    }

    public int getQueueCapacity() {
      return queueCapacity;
    }

    public void setQueueCapacity(int queueCapacity) {
      this.queueCapacity = queueCapacity;
    }
  }

  public static class Notification {
    private Fallback fallback = new Fallback();

    public Fallback getFallback() {
      return fallback;
    }

    public void setFallback(Fallback fallback) {
      this.fallback = fallback;
    }
  }

  public static class Fallback {
    private boolean enabled = true;

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }
  }
}
