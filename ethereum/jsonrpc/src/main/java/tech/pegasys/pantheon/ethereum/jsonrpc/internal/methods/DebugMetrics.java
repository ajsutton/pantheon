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
package tech.pegasys.pantheon.ethereum.jsonrpc.internal.methods;

import tech.pegasys.pantheon.ethereum.jsonrpc.internal.JsonRpcRequest;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.response.JsonRpcResponse;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.response.JsonRpcSuccessResponse;
import tech.pegasys.pantheon.metrics.LabelledMetric;
import tech.pegasys.pantheon.metrics.MetricsSystem;
import tech.pegasys.pantheon.metrics.MetricsSystem.Category;

import java.util.Map;
import java.util.TreeMap;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;
import com.google.common.collect.ImmutableMap;

public class DebugMetrics implements JsonRpcMethod {

  private final MetricsSystem metricsSystem;

  public DebugMetrics(final MetricsSystem metricsSystem) {
    this.metricsSystem = metricsSystem;
  }

  @Override
  public String getName() {
    return "debug_metrics";
  }

  @Override
  public JsonRpcResponse response(final JsonRpcRequest request) {
    final Map<String, Object> metrics = new TreeMap<>();
    for (final Category category : Category.values()) {
      final Map<String, Object> categoryMetrics = getNextMapLevel(metrics, category.getName());
      metricsSystem
          .getMetricNames(category)
          .forEach(
              metricName -> {
                final String name = category.extractRawName(metricName);
                final Metric metric = metricsSystem.getMetric(metricName);
                categoryMetrics.put(name, adaptorFor(metric));
                //                if (metricName.labelNames.isEmpty()) {
                //                  categoryMetrics.put(name, metricName.value);
                //                } else {
                //                  Map<String, Object> values = getNextMapLevel(categoryMetrics,
                // name);
                //                  for (int i = 0; i < metricName.labelValues.size() - 1; i++) {
                //                    values = getNextMapLevel(values,
                // metricName.labelValues.get(i));
                //                  }
                //
                // values.put(metricName.labelValues.get(metricName.labelValues.size() - 1),
                // metricName.value);
                //                }
              });
    }
    return new JsonRpcSuccessResponse(request.getId(), metrics);
  }

  private Object adaptorFor(final Metric metric) {
    if (metric instanceof LabelledMetric) {
      final LabelledMetric<?> labelledMetric = (LabelledMetric<?>) metric;
      final Map<String, Object> labelMetrics = new TreeMap<>();
      labelledMetric
          .getMetrics()
          .forEach(
              (labels, subMetric) -> {
                Map<String, Object> values = labelMetrics;
                for (int i = 0; i < labels.size() - 1; i++) {
                  values = getNextMapLevel(values, labels.get(i));
                }
                values.put(labels.get(labels.size() - 1), adaptorFor(subMetric));
              });
      return labelMetrics;
    } else if (metric instanceof Counter) {
      return ((Counter) metric).getCount();
    } else if (metric instanceof Gauge) {
      return ((Gauge) metric).getValue();
    } else if (metric instanceof Timer) {
      final Timer timer = (Timer) metric;
      final Snapshot snapshot = timer.getSnapshot();

      return new TreeMap<>(
          ImmutableMap.<String, Object>builder()
              .put("min", snapshot.getMin())
              .put("max", snapshot.getMax())
              .put("mean", snapshot.getMean())
              .put("median", snapshot.getMedian())
              .put("stdDev", snapshot.getStdDev())
              .put("oneMinuteRate", timer.getOneMinuteRate())
              .put("fiveMinuteRate", timer.getFiveMinuteRate())
              .put("fifteenMinuteRate", timer.getFifteenMinuteRate())
              .put("p75", snapshot.get75thPercentile())
              .put("p95", snapshot.get95thPercentile())
              .put("p98", snapshot.get98thPercentile())
              .put("p99", snapshot.get99thPercentile())
              .put("p999", snapshot.get999thPercentile())
              .put("meanRate", timer.getMeanRate())
              .put("count", timer.getCount())
              .build());
    }
    return metric;
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> getNextMapLevel(
      final Map<String, Object> current, final String name) {
    return (Map<String, Object>)
        current.computeIfAbsent(name, key -> new TreeMap<String, Object>());
  }
}
