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
package tech.pegasys.pantheon.services.kvstore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import tech.pegasys.pantheon.metrics.Counter;
import tech.pegasys.pantheon.metrics.LabelledMetric;
import tech.pegasys.pantheon.metrics.MetricsSystem;
import tech.pegasys.pantheon.metrics.OperationTimer;
import tech.pegasys.pantheon.metrics.PantheonMetricCategory;
import tech.pegasys.pantheon.metrics.noop.NoOpMetricsSystem;

import java.util.function.LongSupplier;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class RocksDbKeyValueStorageTest extends AbstractKeyValueStorageTest {

  @Mock private MetricsSystem metricsSystemMock;
  @Mock private LabelledMetric<OperationTimer> labelledMetricOperationTimerMock;
  @Mock private LabelledMetric<Counter> labelledMetricCounterMock;
  @Mock private OperationTimer operationTimerMock;
  @Rule public final TemporaryFolder folder = new TemporaryFolder();

  @Override
  protected KeyValueStorage createStore() throws Exception {
    return RocksDbKeyValueStorage.create(config(), new NoOpMetricsSystem());
  }

  @Test
  public void createStoreMustCreateMetrics() throws Exception {
    // Prepare mocks
    when(labelledMetricOperationTimerMock.labels(any())).thenReturn(operationTimerMock);
    when(metricsSystemMock.createLabelledTimer(
            eq(PantheonMetricCategory.KVSTORE_ROCKSDB), anyString(), anyString(), any()))
        .thenReturn(labelledMetricOperationTimerMock);
    when(metricsSystemMock.createLabelledCounter(
            eq(PantheonMetricCategory.KVSTORE_ROCKSDB), anyString(), anyString(), any()))
        .thenReturn(labelledMetricCounterMock);
    // Prepare argument captors
    final ArgumentCaptor<String> labelledTimersMetricsNameArgs =
        ArgumentCaptor.forClass(String.class);
    final ArgumentCaptor<String> labelledTimersHelpArgs = ArgumentCaptor.forClass(String.class);
    final ArgumentCaptor<String> labelledCountersMetricsNameArgs =
        ArgumentCaptor.forClass(String.class);
    final ArgumentCaptor<String> labelledCountersHelpArgs = ArgumentCaptor.forClass(String.class);
    final ArgumentCaptor<String> longGaugesMetricsNameArgs = ArgumentCaptor.forClass(String.class);
    final ArgumentCaptor<String> longGaugesHelpArgs = ArgumentCaptor.forClass(String.class);

    // Actual call
    final KeyValueStorage keyValueStorage =
        RocksDbKeyValueStorage.create(config(), metricsSystemMock);

    // Assertions
    assertThat(keyValueStorage).isNotNull();
    verify(metricsSystemMock, times(4))
        .createLabelledTimer(
            eq(PantheonMetricCategory.KVSTORE_ROCKSDB),
            labelledTimersMetricsNameArgs.capture(),
            labelledTimersHelpArgs.capture(),
            any());
    assertThat(labelledTimersMetricsNameArgs.getAllValues())
        .containsExactly(
            "read_latency_seconds",
            "remove_latency_seconds",
            "write_latency_seconds",
            "commit_latency_seconds");
    assertThat(labelledTimersHelpArgs.getAllValues())
        .containsExactly(
            "Latency for read from RocksDB.",
            "Latency of remove requests from RocksDB.",
            "Latency for write to RocksDB.",
            "Latency for commits to RocksDB.");

    verify(metricsSystemMock, times(2))
        .createLongGauge(
            eq(PantheonMetricCategory.KVSTORE_ROCKSDB),
            longGaugesMetricsNameArgs.capture(),
            longGaugesHelpArgs.capture(),
            any(LongSupplier.class));
    assertThat(longGaugesMetricsNameArgs.getAllValues())
        .containsExactly("rocks_db_table_readers_memory_bytes", "rocks_db_files_size_bytes");
    assertThat(longGaugesHelpArgs.getAllValues())
        .containsExactly(
            "Estimated memory used for RocksDB index and filter blocks in bytes",
            "Estimated database size in bytes");

    verify(metricsSystemMock)
        .createLabelledCounter(
            eq(PantheonMetricCategory.KVSTORE_ROCKSDB),
            labelledCountersMetricsNameArgs.capture(),
            labelledCountersHelpArgs.capture(),
            any());
    assertThat(labelledCountersMetricsNameArgs.getValue()).isEqualTo("rollback_count");
    assertThat(labelledCountersHelpArgs.getValue())
        .isEqualTo("Number of RocksDB transactions rolled back.");
  }

  private RocksDbConfiguration config() throws Exception {
    return RocksDbConfiguration.builder()
        .databaseDir(folder.newFolder().toPath())
        .useColumns(false)
        .build();
  }
}
