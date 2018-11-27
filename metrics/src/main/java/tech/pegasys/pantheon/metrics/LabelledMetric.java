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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import com.codahale.metrics.Metric;

public class LabelledMetric<T extends Metric> implements Metric {

  private final String baseName;
  private final Function<String, T> metricSupplier;
  private final Map<List<String>, T> metrics = new ConcurrentHashMap<>();

  public LabelledMetric(final String baseName, final Function<String, T> metricSupplier) {
    this.baseName = baseName;
    this.metricSupplier = metricSupplier;
  }

  public T labels(final String... labels) {
    final String name = combinedName(labels);
    final T metric = metricSupplier.apply(name);
    metrics.put(asList(labels), metric);
    return metric;
  }

  public Map<List<String>, Metric> getMetrics() {
    return Collections.unmodifiableMap(metrics);
  }

  private String combinedName(final String... labels) {
    return baseName + Arrays.toString(labels);
  }
}
