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

import static java.util.concurrent.CompletableFuture.completedFuture;

import tech.pegasys.pantheon.ethereum.ProtocolContext;
import tech.pegasys.pantheon.ethereum.eth.manager.EthContext;
import tech.pegasys.pantheon.ethereum.eth.manager.EthScheduler;
import tech.pegasys.pantheon.ethereum.eth.sync.SynchronizerConfiguration;
import tech.pegasys.pantheon.ethereum.eth.sync.tasks.WaitForPeerTask;
import tech.pegasys.pantheon.ethereum.eth.sync.tasks.WaitForPeersTask;
import tech.pegasys.pantheon.ethereum.mainnet.ProtocolSchedule;
import tech.pegasys.pantheon.metrics.LabelledMetric;
import tech.pegasys.pantheon.metrics.OperationTimer;
import tech.pegasys.pantheon.util.ExceptionUtils;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeoutException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FastSyncActions<C> {

  private static final Logger LOG = LogManager.getLogger();
  private final SynchronizerConfiguration syncConfig;
  private final ProtocolSchedule<C> protocolSchedule;
  private final ProtocolContext<C> protocolContext;
  private final EthContext ethContext;
  private final LabelledMetric<OperationTimer> ethTasksTimer;

  public FastSyncActions(
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

  public CompletableFuture<FastSyncResult> waitForSuitablePeers() {
    final WaitForPeersTask waitForPeersTask =
        WaitForPeersTask.create(
            ethContext, syncConfig.getFastSyncMinimumPeerCount(), ethTasksTimer);

    final EthScheduler scheduler = ethContext.getScheduler();
    return scheduler
        .timeout(waitForPeersTask, syncConfig.getFastSyncMaximumPeerWaitTime())
        .handle(
            (result, error) -> {
              if (ExceptionUtils.rootCause(error) instanceof TimeoutException) {
                if (ethContext.getEthPeers().bestPeer().isPresent()) {
                  LOG.warn(
                      "Fast sync timed out before minimum peer count was reached. Continuing with reduced peers.");
                } else {
                  return FastSyncResult.NO_PEERS_AVAILABLE;
                }
              } else if (error != null) {
                LOG.error("Failed to find peers for fast sync", error);
                return FastSyncResult.UNEXPECTED_ERROR;
              }
              return FastSyncResult.SUCCESS;
            })
        .thenCompose(
            result ->
                result == FastSyncResult.NO_PEERS_AVAILABLE
                    ? waitForAnyPeer()
                    : completedFuture(result));
  }

  private CompletionStage<FastSyncResult> waitForAnyPeer() {
    LOG.warn(
        "Maximum wait time for fast sync reached but no peers available. Continuing to wait for any available peer.");
    final WaitForPeerTask waitForPeerTask = WaitForPeerTask.create(ethContext, ethTasksTimer);
    return ethContext
        .getScheduler()
        .scheduleSyncWorkerTask(waitForPeerTask::run)
        .thenApply(voidResult -> FastSyncResult.SUCCESS);
  }
}
