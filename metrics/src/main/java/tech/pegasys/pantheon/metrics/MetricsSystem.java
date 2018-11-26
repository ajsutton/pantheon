/*
 * Copyright 2018 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package tech.pegasys.pantheon.metrics;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import io.prometheus.client.Collector;
import io.prometheus.client.Collector.MetricFamilySamples;
import io.prometheus.client.Counter;
import io.prometheus.client.hotspot.BufferPoolsExports;
import io.prometheus.client.hotspot.ClassLoadingExports;
import io.prometheus.client.hotspot.GarbageCollectorExports;
import io.prometheus.client.hotspot.MemoryPoolsExports;
import io.prometheus.client.hotspot.StandardExports;
import io.prometheus.client.hotspot.ThreadExports;

public class MetricsSystem {

  private final Map<Category, Collection<Collector>> collectors = new ConcurrentHashMap<>();

  public static MetricsSystem init() {
    final MetricsSystem metricsSystem = new MetricsSystem();
    metricsSystem.collectors.put(Category.PROCESS, singleton(new StandardExports()));
    metricsSystem.collectors.put(
        Category.JVM,
        asList(
            new MemoryPoolsExports(),
            new BufferPoolsExports(),
            new GarbageCollectorExports(),
            new ThreadExports(),
            new ClassLoadingExports()));
    return metricsSystem;
  }

  public Counter createCounter(
      final Category category, final String name, final String help, final String... labelNames) {
    final Counter counter =
        Counter.build(category.getNameForMetric(name), help).labelNames(labelNames).create();
    collectors.computeIfAbsent(category, key -> concurrentSet()).add(counter);
    return counter;
  }

  private Collection<Collector> concurrentSet() {
    return Collections.newSetFromMap(new ConcurrentHashMap<>());
  }

  public Stream<MetricFamilySamples> getMetrics() {
    return collectors
        .values()
        .stream()
        .flatMap(Collection::stream)
        .flatMap(collector -> collector.collect().stream());
  }

  public Stream<MetricFamilySamples> getMetrics(final Category category) {
    return collectors
        .getOrDefault(category, Collections.emptySet())
        .stream()
        .flatMap(collector -> collector.collect().stream());
  }

  public enum Category {
    PEERS("peers"),
    JVM("jvm", ""),
    PROCESS("process", "");

    private final String name;
    private final String prefix;

    Category(final String name) {
      this(name, "pantheon_");
    }

    Category(final String name, final String categoryPrefix) {
      this.name = name;
      this.prefix = categoryPrefix + name + "_";
    }

    public String getName() {
      return name;
    }

    public String getNameForMetric(final String metricName) {
      return prefix + metricName;
    }

    public String extractRawName(final String metricName) {
      return metricName.startsWith(prefix) ? metricName.substring(prefix.length()) : metricName;
    }
  }
}
