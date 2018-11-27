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

import tech.pegasys.pantheon.metrics.MetricsSystem.Category;

import java.util.List;

public class Observation {
  private final Category category;
  private final String metricName;
  private final List<String> labels;
  private final double value;

  public Observation(
      final Category category,
      final String metricName,
      final double value,
      final List<String> labels) {
    this.category = category;
    this.metricName = metricName;
    this.value = value;
    this.labels = labels;
  }

  public Category getCategory() {
    return category;
  }

  public String getMetricName() {
    return metricName;
  }

  public List<String> getLabels() {
    return labels;
  }

  public double getValue() {
    return value;
  }
}
