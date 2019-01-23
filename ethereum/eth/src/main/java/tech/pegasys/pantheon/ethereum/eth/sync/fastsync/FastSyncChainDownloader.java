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
import tech.pegasys.pantheon.ethereum.core.Block;
import tech.pegasys.pantheon.ethereum.core.BlockHeader;
import tech.pegasys.pantheon.ethereum.core.Hash;
import tech.pegasys.pantheon.ethereum.eth.manager.AbstractPeerTask.PeerTaskResult;
import tech.pegasys.pantheon.ethereum.eth.manager.EthContext;
import tech.pegasys.pantheon.ethereum.eth.manager.EthPeer;
import tech.pegasys.pantheon.ethereum.eth.manager.EthTask;
import tech.pegasys.pantheon.ethereum.eth.sync.GenericDownloader;
import tech.pegasys.pantheon.ethereum.eth.sync.GenericDownloader.SyncTargetManager;
import tech.pegasys.pantheon.ethereum.eth.sync.SynchronizerConfiguration;
import tech.pegasys.pantheon.ethereum.eth.sync.state.SyncState;
import tech.pegasys.pantheon.ethereum.eth.sync.state.SyncTarget;
import tech.pegasys.pantheon.ethereum.eth.sync.tasks.GetHeadersFromPeerByHashTask;
import tech.pegasys.pantheon.ethereum.eth.sync.tasks.WaitForPeerTask;
import tech.pegasys.pantheon.ethereum.mainnet.ProtocolSchedule;
import tech.pegasys.pantheon.metrics.LabelledMetric;
import tech.pegasys.pantheon.metrics.OperationTimer;

import java.time.Duration;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FastSyncChainDownloader<C> {
  private static final Logger LOG = LogManager.getLogger();
  private final GenericDownloader<C> genericDownloader;
  private final SynchronizerConfiguration config;
  private final ProtocolSchedule<C> protocolSchedule;
  private final ProtocolContext<C> protocolContext;
  private final EthContext ethContext;
  private final SyncState syncState;
  private final LabelledMetric<OperationTimer> ethTasksTimer;
  private final BlockHeader pivotBlockHeader;
  // TODO: Ultimately should store this so that we can resume fast sync download.
  private volatile Hash lastImportedBlockHash;

  FastSyncChainDownloader(
      final SynchronizerConfiguration config,
      final ProtocolSchedule<C> protocolSchedule,
      final ProtocolContext<C> protocolContext,
      final EthContext ethContext,
      final SyncState syncState,
      final LabelledMetric<OperationTimer> ethTasksTimer,
      final BlockHeader pivotBlockHeader) {
    this.config = config;
    this.protocolSchedule = protocolSchedule;
    this.protocolContext = protocolContext;
    this.ethContext = ethContext;
    this.syncState = syncState;
    this.ethTasksTimer = ethTasksTimer;
    this.pivotBlockHeader = pivotBlockHeader;
    this.lastImportedBlockHash = pivotBlockHeader.getHash();
    genericDownloader =
        new GenericDownloader<>(
            config,
            protocolContext,
            ethContext,
            syncState,
            ethTasksTimer,
            new FastSyncTargetManager(),
            this::checkpointHeadersTask,
            this::importBlocksForCheckpoints);
  }

  public void start() {
    genericDownloader.start();
  }

  private EthTask<PeerTaskResult<List<BlockHeader>>> checkpointHeadersTask(
      final BlockHeader lastHeader, final SyncTarget syncTarget) {
    LOG.debug("Requesting checkpoint headers from {}", lastHeader.getNumber());
    return GetHeadersFromPeerByHashTask.endingAtHash(
            protocolSchedule,
            ethContext,
            lastHeader.getHash(),
            lastHeader.getNumber(),
            config.downloaderHeaderRequestSize() + 1,
            config.downloaderChainSegmentSize() - 1,
            ethTasksTimer)
        .assignPeer(syncTarget.peer());
  }

  private CompletableFuture<List<Block>> importBlocksForCheckpoints(
      final Deque<BlockHeader> checkpointHeaders) {

    throw new UnsupportedOperationException("Not implemented");
  }

  private class FastSyncTargetManager implements SyncTargetManager {

    private long syncTargetDisconnectListenerId;
    private volatile boolean syncTargetDisconnected = false;

    @Override
    public CompletableFuture<SyncTarget> findSyncTarget() {
      final Optional<EthPeer> maybeBestPeer = ethContext.getEthPeers().bestPeer();
      if (!maybeBestPeer.isPresent()) {
        LOG.info("No sync target, wait for peers.");
        return waitForPeerAndThenSetSyncTarget();
      } else {
        final EthPeer bestPeer = maybeBestPeer.get();
        if (bestPeer.chainState().getEstimatedHeight() < pivotBlockHeader.getNumber()) {
          LOG.info("No sync target with sufficient chain height, wait for peers.");
          return waitForPeerAndThenSetSyncTarget();
        }
        syncTargetDisconnectListenerId =
            bestPeer.subscribeDisconnect(this::onSyncTargetPeerDisconnect);
        final BlockHeader commonAncestor =
            protocolContext
                .getBlockchain()
                .getBlockHeader(lastImportedBlockHash)
                .orElse(pivotBlockHeader);
        final SyncTarget syncTarget = syncState.setSyncTarget(bestPeer, commonAncestor);
        return CompletableFuture.completedFuture(syncTarget);
      }
    }

    @Override
    public boolean isSyncTargetDisconnected() {
      return syncTargetDisconnected;
    }

    @Override
    public void clearSyncTarget(final SyncTarget syncTarget) {
      syncTarget.peer().unsubscribeDisconnect(syncTargetDisconnectListenerId);
      syncTargetDisconnected = false;
    }

    @Override
    public boolean shouldSwitchSyncTarget(final SyncTarget currentTarget) {
      return false;
    }

    private CompletableFuture<SyncTarget> waitForPeerAndThenSetSyncTarget() {
      return waitForNewPeer().handle((r, t) -> r).thenCompose((r) -> findSyncTarget());
    }

    private CompletableFuture<?> waitForNewPeer() {
      return ethContext
          .getScheduler()
          .timeout(WaitForPeerTask.create(ethContext, ethTasksTimer), Duration.ofSeconds(5));
    }

    private void onSyncTargetPeerDisconnect(final EthPeer ethPeer) {
      LOG.info("Sync target disconnected: {}", ethPeer);
      syncTargetDisconnected = true;
    }
  }
}
