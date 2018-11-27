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

import java.util.function.Supplier;
import java.util.stream.Stream;

public interface MetricsSystem {

  Counter createCounter(Category category, String name, String help);

  LabelledMetric<Counter> createCounter(
      Category category, String name, String help, String... labelNames);

  LabelledMetric<OperationTimer> createTimer(
      Category category, String name, String help, String... labelNames);

  void createGauge(Category category, String name, String help, Supplier<Double> valueSupplier);

  Stream<Observation> getMetrics(Category category);

  default Stream<Observation> getMetrics() {
    return Stream.of(Category.values()).flatMap(this::getMetrics);
  }

  enum Category {
    PEERS("peers"),
    RPC("rpc"),
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
