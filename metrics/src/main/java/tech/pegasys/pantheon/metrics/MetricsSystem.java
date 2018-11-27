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

import java.lang.management.ManagementFactory;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Stream;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricSet;
import com.codahale.metrics.Timer;
import com.codahale.metrics.jmx.JmxReporter;
import com.codahale.metrics.jvm.BufferPoolMetricSet;
import com.codahale.metrics.jvm.ClassLoadingGaugeSet;
import com.codahale.metrics.jvm.FileDescriptorRatioGauge;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.JvmAttributeGaugeSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.codahale.metrics.jvm.ThreadStatesGaugeSet;

public class MetricsSystem {

  private final MetricRegistry metrics = new MetricRegistry();
  private final Map<Category, Collection<String>> collectors = new ConcurrentHashMap<>();
  private final Map<String, LabelledMetric<?>> labelledMetrics = new ConcurrentHashMap<>();

  public static MetricsSystem init() {
    final MetricsSystem metricsSystem = new MetricsSystem();
    //    metricsSystem.collectors.put(Category.PROCESS, singleton(new StandardExports()));
    //    new FileDescriptorRatioGauge(),
    metricsSystem.register(
        Category.JVM,
        new ThreadStatesGaugeSet(),
        new BufferPoolMetricSet(ManagementFactory.getPlatformMBeanServer()),
        new GarbageCollectorMetricSet(),
        new MemoryUsageGaugeSet(),
        new ClassLoadingGaugeSet(),
        new JvmAttributeGaugeSet());
    metricsSystem.metrics.register("process_file_descriptor_ratio", new FileDescriptorRatioGauge());
    metricsSystem.addCollector(Category.PROCESS, "process_file_descriptor_ratio");

    final JmxReporter jmxReporter = JmxReporter.forRegistry(metricsSystem.metrics).build();
    jmxReporter.start();
    return metricsSystem;
  }

  private void register(final Category category, final MetricSet... metricSets) {
    for (final MetricSet metricSet : metricSets) {
      metrics.registerAll(metricSet);
      metricSet.getMetrics().keySet().forEach(metricName -> addCollector(category, metricName));
    }
  }

  public Counter createCounter(final Category category, final String name) {

    final String metricName = category.getNameForMetric(name);
    final Counter counter = metrics.counter(metricName);
    addCollector(category, metricName);
    return counter;
  }

  public LabelledMetric<Counter> createCounter(
      final Category category, final String name, final String... labelNames) {
    return createLabelledMetric(category, name, metrics::counter);
  }

  public Timer createTimer(final Category category, final String name) {
    final String metricName = category.getNameForMetric(name);
    final Timer timer = metrics.timer(metricName);
    addCollector(category, metricName);
    return timer;
  }

  public LabelledMetric<Timer> createTimer(
      final Category category, final String name, final String... labelNames) {
    return createLabelledMetric(category, name, metrics::timer);
  }

  @SuppressWarnings("unchecked")
  public <T> Gauge<T> createGauge(
      final Category category, final String name, final Gauge<T> collector) {
    final String metricName = category.getNameForMetric(name);
    final Gauge<T> gauge = metrics.gauge(metricName, () -> collector);
    addCollector(category, metricName);
    return gauge;
  }

  private <T extends Metric> LabelledMetric<T> createLabelledMetric(
      final Category category, final String name, final Function<String, T> metricFactory) {
    final String metricName = category.getNameForMetric(name);
    addCollector(category, metricName);
    final LabelledMetric<T> labelledMetric = new LabelledMetric<>(metricName, metricFactory);
    labelledMetrics.put(metricName, labelledMetric);
    return labelledMetric;
  }

  private void addCollector(final Category category, final String name) {
    collectors
        .computeIfAbsent(category, key -> Collections.newSetFromMap(new ConcurrentHashMap<>()))
        .add(name);
  }

  public Stream<String> getMetricNames(final Category category) {
    return collectors.getOrDefault(category, Collections.emptySet()).stream();
  }

  public Metric getMetric(final String name) {
    final LabelledMetric<?> labelledMetric = labelledMetrics.get(name);
    if (labelledMetric != null) {
      return labelledMetric;
    }
    return metrics.getMetrics().get(name);
  }

  public MetricRegistry getMetrics() {
    return metrics;
  }

  public enum Category {
    PEERS("peers"),
    RPC("rpc"),
    JVM("jvm", ""),
    PROCESS("process", ""),
    LOGS("logs");

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
