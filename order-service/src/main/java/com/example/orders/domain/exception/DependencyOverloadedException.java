package com.example.orders.domain.exception;

public class DependencyOverloadedException extends RuntimeException {
  private final String dependency;

  public DependencyOverloadedException(String dependency, Throwable cause) {
    super(dependency + " is overloaded", cause);
    this.dependency = dependency;
  }

  public String getDependency() {
    return dependency;
  }
}
