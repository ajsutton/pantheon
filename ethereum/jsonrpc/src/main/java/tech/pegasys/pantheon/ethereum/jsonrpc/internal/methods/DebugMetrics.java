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
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.response.JsonRpcError;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.response.JsonRpcErrorResponse;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.response.JsonRpcResponse;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.response.JsonRpcSuccessResponse;
import tech.pegasys.pantheon.metrics.MetricsSystem;
import tech.pegasys.pantheon.metrics.MetricsSystem.Category;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DebugMetrics implements JsonRpcMethod {

  private static final Logger LOG = LogManager.getLogger();

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
    final Map<String, Object> metrics = new HashMap<>();

    try {
      metricsSystem
          .getMetrics()
          .forEach(
              sample -> {
                final Category category = sample.getCategory();
                final Map<String, Object> categoryMetrics =
                    getNextMapLevel(metrics, category.getName());
                final String name = category.extractRawName(sample.getMetricName());
                final List<String> labels = sample.getLabels();
                if (labels.isEmpty()) {
                  categoryMetrics.put(name, sample.getValue());
                } else {
                  Map<String, Object> values = getNextMapLevel(categoryMetrics, name);
                  for (int i = 0; i < labels.size() - 1; i++) {
                    values = getNextMapLevel(values, labels.get(i));
                  }
                  values.put(labels.get(labels.size() - 1), sample.getValue());
                }
              });
      return new JsonRpcSuccessResponse(request.getId(), metrics);
    } catch (Exception e) {
      LOG.error("Failed to serialize metrics", e);
      return new JsonRpcErrorResponse(request.getId(), JsonRpcError.INTERNAL_ERROR);
    }
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> getNextMapLevel(
      final Map<String, Object> current, final String name) {
    return (Map<String, Object>)
        current.computeIfAbsent(name, key -> new HashMap<String, Object>());
  }
}
