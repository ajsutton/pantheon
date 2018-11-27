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
package tech.pegasys.pantheon.metrics.noop;

import tech.pegasys.pantheon.metrics.Counter;
import tech.pegasys.pantheon.metrics.LabelledMetric;
import tech.pegasys.pantheon.metrics.MetricsSystem;
import tech.pegasys.pantheon.metrics.Observation;
import tech.pegasys.pantheon.metrics.OperationTimer;

import java.util.function.Supplier;
import java.util.stream.Stream;

public class NoOpMetricsSystem implements MetricsSystem {

  private static final Counter NO_OP_COUNTER = () -> {};

  @Override
  public Counter createCounter(final Category category, final String name, final String help) {
    return NO_OP_COUNTER;
  }

  @Override
  public LabelledMetric<Counter> createCounter(
      final Category category, final String name, final String help, final String... labelNames) {
    return labels -> NO_OP_COUNTER;
  }

  @Override
  public LabelledMetric<OperationTimer> createTimer(
      final Category category, final String name, final String help, final String... labelNames) {
    return labels -> () -> () -> {};
  }

  @Override
  public void createGauge(
      final Category category,
      final String name,
      final String help,
      final Supplier<Double> valueSupplier) {}

  @Override
  public Stream<Observation> getMetrics(final Category category) {
    return Stream.empty();
  }

  @Override
  public Stream<Observation> getMetrics() {
    return Stream.empty();
  }
}
