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

import tech.pegasys.pantheon.metrics.LabelledMetric;
import tech.pegasys.pantheon.metrics.OperationTimer;

import io.prometheus.client.Histogram;
import io.prometheus.client.Histogram.Child;

public class PrometheusTimer implements LabelledMetric<OperationTimer> {

  private final Histogram histogram;

  public PrometheusTimer(final Histogram histogram) {
    this.histogram = histogram;
  }

  @Override
  public OperationTimer labels(final String... labels) {
    final Child metric = histogram.labels(labels);
    return () -> metric.startTimer()::observeDuration;
  }
}
