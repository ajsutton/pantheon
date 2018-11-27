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
package tech.pegasys.pantheon.metrics.prometheus;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;

import tech.pegasys.pantheon.metrics.LabelledMetric;
import tech.pegasys.pantheon.metrics.MetricsSystem;
import tech.pegasys.pantheon.metrics.Observation;
import tech.pegasys.pantheon.metrics.OperationTimer;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Stream;

import io.prometheus.client.Collector;
import io.prometheus.client.Collector.MetricFamilySamples;
import io.prometheus.client.Collector.MetricFamilySamples.Sample;
import io.prometheus.client.Counter;
import io.prometheus.client.Histogram;
import io.prometheus.client.hotspot.BufferPoolsExports;
import io.prometheus.client.hotspot.ClassLoadingExports;
import io.prometheus.client.hotspot.GarbageCollectorExports;
import io.prometheus.client.hotspot.MemoryPoolsExports;
import io.prometheus.client.hotspot.StandardExports;
import io.prometheus.client.hotspot.ThreadExports;

public class PrometheusMetricsSystem implements MetricsSystem {

  private static final String[] NO_LABELS = new String[0];
  private final Map<Category, Collection<Collector>> collectors = new ConcurrentHashMap<>();

  private PrometheusMetricsSystem() {}

  public static MetricsSystem init() {
    final PrometheusMetricsSystem metricsSystem = new PrometheusMetricsSystem();
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

  @Override
  public tech.pegasys.pantheon.metrics.Counter createCounter(
      final Category category, final String name, final String help) {
    return createCounter(category, name, help, NO_LABELS).labels();
  }

  @Override
  public LabelledMetric<tech.pegasys.pantheon.metrics.Counter> createCounter(
      final Category category, final String name, final String help, final String... labelNames) {
    final Counter counter =
        Counter.build(category.getNameForMetric(name), help).labelNames(labelNames).create();
    addCollector(category, counter);
    return new PrometheusCounter(counter);
  }

  @Override
  public LabelledMetric<OperationTimer> createTimer(
      final Category category, final String name, final String help, final String... labelNames) {
    final Histogram histogram = Histogram.build(name, help).labelNames(labelNames).create();
    addCollector(category, histogram);
    return new PrometheusTimer(histogram);
  }

  @Override
  public void createGauge(
      final Category category,
      final String name,
      final String help,
      final Supplier<Double> valueSupplier) {
    final String metricName = category.getNameForMetric(name);
    addCollector(
        category,
        new Collector() {
          @Override
          public List<MetricFamilySamples> collect() {
            return singletonList(
                new MetricFamilySamples(
                    metricName,
                    Type.GAUGE,
                    help,
                    singletonList(
                        new Sample(metricName, emptyList(), emptyList(), valueSupplier.get()))));
          }
        });
  }

  private void addCollector(final Category category, final Collector counter) {
    collectors
        .computeIfAbsent(category, key -> Collections.newSetFromMap(new ConcurrentHashMap<>()))
        .add(counter);
  }

  @Override
  public Stream<Observation> getMetrics(final Category category) {
    return collectors
        .getOrDefault(category, Collections.emptySet())
        .stream()
        .flatMap(collector -> collector.collect().stream())
        .flatMap(familySample -> convertSamplesToObservations(category, familySample));
  }

  private Stream<Observation> convertSamplesToObservations(
      final Category category, final MetricFamilySamples familySample) {
    return familySample
        .samples
        .stream()
        .map(
            sample ->
                new Observation(
                    category,
                    category.extractRawName(familySample.name),
                    sample.value,
                    sample.labelValues));
  }
}
