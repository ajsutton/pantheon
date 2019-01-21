/*
 * Copyright 2019 ConsenSys AG.
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
package tech.pegasys.pantheon.ethereum.eth.sync.fastsync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static tech.pegasys.pantheon.metrics.noop.NoOpMetricsSystem.NO_OP_LABELLED_TIMER;

import tech.pegasys.pantheon.ethereum.ProtocolContext;
import tech.pegasys.pantheon.ethereum.eth.manager.EthProtocolManager;
import tech.pegasys.pantheon.ethereum.eth.manager.EthProtocolManagerTestUtil;
import tech.pegasys.pantheon.ethereum.eth.sync.SyncMode;
import tech.pegasys.pantheon.ethereum.eth.sync.SynchronizerConfiguration;
import tech.pegasys.pantheon.ethereum.mainnet.ProtocolSchedule;
import tech.pegasys.pantheon.metrics.LabelledMetric;
import tech.pegasys.pantheon.metrics.OperationTimer;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Before;
import org.junit.Test;

public class FastSyncActionsTest {

  private final SynchronizerConfiguration syncConfig =
      new SynchronizerConfiguration.Builder().syncMode(SyncMode.FAST).build();

  @SuppressWarnings("unchecked")
  private final ProtocolSchedule<Void> protocolSchedule = mock(ProtocolSchedule.class);

  @SuppressWarnings("unchecked")
  private final ProtocolContext<Void> protocolContext = mock(ProtocolContext.class);

  private final LabelledMetric<OperationTimer> ethTasksTimer = NO_OP_LABELLED_TIMER;
  private final AtomicBoolean timeout = new AtomicBoolean(false);
  private FastSyncActions<Void> fastSyncActions;
  private EthProtocolManager ethProtocolManager;

  @Before
  public void setUp() {
    ethProtocolManager = EthProtocolManagerTestUtil.create(timeout::get);
    fastSyncActions =
        new FastSyncActions<>(
            syncConfig,
            protocolSchedule,
            protocolContext,
            ethProtocolManager.ethContext(),
            ethTasksTimer);
  }

  @Test
  public void waitForPeersShouldSucceedIfEnoughPeersAreFound() {
    for (int i = 0; i < syncConfig.getFastSyncMinimumPeerCount(); i++) {
      EthProtocolManagerTestUtil.createPeer(ethProtocolManager);
    }
    final CompletableFuture<FastSyncResult> result = fastSyncActions.waitForSuitablePeers();
    assertThat(result).isCompletedWithValue(FastSyncResult.SUCCESS);
  }

  @Test
  public void waitForPeersShouldReportSuccessWhenTimeLimitReachedAndAPeerIsAvailable() {
    EthProtocolManagerTestUtil.createPeer(ethProtocolManager);
    timeout.set(true);
    final CompletableFuture<FastSyncResult> result = fastSyncActions.waitForSuitablePeers();
    assertThat(result).isCompletedWithValue(FastSyncResult.SUCCESS);
  }

  @Test
  public void waitForPeersShouldContinueWaitingUntilAtLeastOnePeerIsAvailable() {
    timeout.set(true);
    final CompletableFuture<FastSyncResult> result = fastSyncActions.waitForSuitablePeers();
    assertThat(result).isNotCompleted();

    EthProtocolManagerTestUtil.createPeer(ethProtocolManager);
    assertThat(result).isCompletedWithValue(FastSyncResult.SUCCESS);
  }
}
