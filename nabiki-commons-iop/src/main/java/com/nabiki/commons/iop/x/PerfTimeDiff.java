package com.nabiki.commons.iop.x;

import java.time.Duration;
import java.util.Map;

public class PerfTimeDiff {
  private final String name;
  private final long startNanos;
  private final Map<String, Duration> measures;

  PerfTimeDiff(String name, Map<String, Duration> measures) {
    this.name = name;
    this.measures = measures;
    this.startNanos = System.nanoTime();
  }

  public Duration end() {
    var duration = Duration.ofNanos(System.nanoTime() - this.startNanos);
    measures.put(this.name, duration);
    return duration;
  }

  public Duration compareAndEnd(int positive) {
    var duration = Duration.ofNanos(System.nanoTime() - this.startNanos);
    var old = measures.get(this.name);
    if (old == null || old.compareTo(duration) * positive < 0)
      measures.put(this.name, duration);
    else
      duration = measures.get(this.name);
    return duration;
  }

  public Duration endWithMax() {
    return compareAndEnd(1);
  }

  public Duration endWithMin() {
    return compareAndEnd(-1);
  }
}
