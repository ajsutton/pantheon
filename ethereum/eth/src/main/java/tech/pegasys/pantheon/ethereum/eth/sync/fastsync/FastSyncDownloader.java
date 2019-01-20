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

import tech.pegasys.pantheon.ethereum.ProtocolContext;
import tech.pegasys.pantheon.ethereum.eth.manager.EthContext;
import tech.pegasys.pantheon.ethereum.eth.sync.SynchronizerConfiguration;
import tech.pegasys.pantheon.ethereum.eth.sync.tasks.WaitForPeersTask;
import tech.pegasys.pantheon.ethereum.mainnet.ProtocolSchedule;
import tech.pegasys.pantheon.metrics.LabelledMetric;
import tech.pegasys.pantheon.metrics.OperationTimer;
import tech.pegasys.pantheon.util.ExceptionUtils;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FastSyncDownloader<C> {
  private static final Logger LOG = LogManager.getLogger();
  private final SynchronizerConfiguration syncConfig;
  private final ProtocolSchedule<C> protocolSchedule;
  private final ProtocolContext<C> protocolContext;
  private final EthContext ethContext;
  private final LabelledMetric<OperationTimer> ethTasksTimer;

  public FastSyncDownloader(
      final SynchronizerConfiguration syncConfig,
      final ProtocolSchedule<C> protocolSchedule,
      final ProtocolContext<C> protocolContext,
      final EthContext ethContext,
      final LabelledMetric<OperationTimer> ethTasksTimer) {
    this.syncConfig = syncConfig;
    this.protocolSchedule = protocolSchedule;
    this.protocolContext = protocolContext;
    this.ethContext = ethContext;
    this.ethTasksTimer = ethTasksTimer;
  }

  public CompletableFuture<FastSyncResult> start() {
    LOG.info("Fast sync enabled");
    return waitForSuitablePeers().handle((result, error) -> handleWaitForPeersResult(error));
  }

  private FastSyncResult handleWaitForPeersResult(final Throwable error) {
    if (ExceptionUtils.rootCause(error) instanceof TimeoutException) {
      LOG.warn(
          "Fast sync timed out before minimum peer count was reached. Continuing with reduced peers.");
    } else if (error != null) {
      LOG.error("Failed to find peers for fast sync", error);
      return FastSyncResult.UNEXPECTED_ERROR;
    }
    return FastSyncResult.FAST_SYNC_UNAVAILABLE;
  }

  private CompletableFuture<Void> waitForSuitablePeers() {
    final WaitForPeersTask waitForPeersTask =
        WaitForPeersTask.create(ethContext, syncConfig.fastSyncMinimumPeerCount(), ethTasksTimer);
    return ethContext.getScheduler().scheduleSyncWorkerTask(waitForPeersTask::run);
  }
}
