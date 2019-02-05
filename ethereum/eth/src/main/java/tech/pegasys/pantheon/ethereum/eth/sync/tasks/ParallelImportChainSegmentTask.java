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
package tech.pegasys.pantheon.ethereum.eth.sync.tasks;

import tech.pegasys.pantheon.ethereum.ProtocolContext;
import tech.pegasys.pantheon.ethereum.core.BlockHeader;
import tech.pegasys.pantheon.ethereum.eth.manager.AbstractPeerTask;
import tech.pegasys.pantheon.ethereum.eth.manager.EthContext;
import tech.pegasys.pantheon.ethereum.eth.manager.EthPeer;
import tech.pegasys.pantheon.ethereum.eth.manager.EthScheduler;
import tech.pegasys.pantheon.ethereum.eth.sync.BlockHandler;
import tech.pegasys.pantheon.ethereum.eth.sync.ValidationPolicy;
import tech.pegasys.pantheon.ethereum.mainnet.ProtocolSchedule;
import tech.pegasys.pantheon.metrics.LabelledMetric;
import tech.pegasys.pantheon.metrics.OperationTimer;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ParallelImportChainSegmentTask<C, B> extends AbstractPeerTask<List<B>> {
  private static final Logger LOG = LogManager.getLogger();

  private final ProtocolSchedule<C> protocolSchedule;
  private final ProtocolContext<C> protocolContext;

  private final ArrayBlockingQueue<BlockHeader> checkpointHeaders;
  private final int maxActiveChunks;
  private final long firstHeaderNumber;
  private final long lastHeaderNumber;

  private final BlockHandler<B> blockHandler;
  private final ValidationPolicy validationPolicy;

  private ParallelImportChainSegmentTask(
      final ProtocolSchedule<C> protocolSchedule,
      final ProtocolContext<C> protocolContext,
      final EthContext ethContext,
      final int maxActiveChunks,
      final List<BlockHeader> checkpointHeaders,
      final LabelledMetric<OperationTimer> ethTasksTimer,
      final BlockHandler<B> blockHandler,
      final ValidationPolicy validationPolicy) {
    super(ethContext, ethTasksTimer);
    this.protocolSchedule = protocolSchedule;
    this.protocolContext = protocolContext;
    this.maxActiveChunks = maxActiveChunks;

    if (checkpointHeaders.size() > 1) {
      this.firstHeaderNumber = checkpointHeaders.get(0).getNumber();
      this.lastHeaderNumber = checkpointHeaders.get(checkpointHeaders.size() - 1).getNumber();
    } else {
      this.firstHeaderNumber = -1;
      this.lastHeaderNumber = -1;
    }
    this.checkpointHeaders =
        new ArrayBlockingQueue<>(checkpointHeaders.size(), false, checkpointHeaders);
    //    this.chunksInTotal = checkpointHeaders.size() - 1;
    this.blockHandler = blockHandler;
    this.validationPolicy = validationPolicy;
  }

  public static <C, B> ParallelImportChainSegmentTask<C, B> forCheckpoints(
      final ProtocolSchedule<C> protocolSchedule,
      final ProtocolContext<C> protocolContext,
      final EthContext ethContext,
      final int maxActiveChunks,
      final LabelledMetric<OperationTimer> ethTasksTimer,
      final BlockHandler<B> blockHandler,
      final ValidationPolicy validationPolicy,
      final List<BlockHeader> checkpointHeaders) {
    return new ParallelImportChainSegmentTask<>(
        protocolSchedule,
        protocolContext,
        ethContext,
        maxActiveChunks,
        checkpointHeaders,
        ethTasksTimer,
        blockHandler,
        validationPolicy);
  }

  @Override
  protected void executeTaskWithPeer(final EthPeer peer) {
    if (firstHeaderNumber >= 0) {
      LOG.debug("Importing chain segment from {} to {}.", firstHeaderNumber, lastHeaderNumber);

      // build pipeline
      final ParallelDownloadHeadersTask<C> downloadTask =
          new ParallelDownloadHeadersTask<>(
              checkpointHeaders,
              maxActiveChunks,
              protocolSchedule,
              protocolContext,
              ethContext,
              ethTasksTimer);
      final ParallelValidateHeadersTask<C> validateTask =
          new ParallelValidateHeadersTask<>(
              validationPolicy,
              downloadTask.getOutboundQueue(),
              maxActiveChunks,
              protocolSchedule,
              protocolContext,
              ethContext,
              ethTasksTimer);
      final ParallelDownloadBodiesTask<B> downloadBodiesTask =
          new ParallelDownloadBodiesTask<>(
              blockHandler,
              validateTask.getOutboundQueue(),
              maxActiveChunks,
              ethContext,
              ethTasksTimer);
      final ParallelValidateAndImportBodiesTask<B> validateAndImportBodiesTask =
          new ParallelValidateAndImportBodiesTask<>(
              blockHandler,
              downloadBodiesTask.getOutboundQueue(),
              maxActiveChunks,
              ethContext,
              ethTasksTimer);

      // Start the pipeline.
      final EthScheduler scheduler = ethContext.getScheduler();
      final CompletableFuture<?> downloadHeaderFuture = scheduler.scheduleServiceTask(downloadTask);
      final CompletableFuture<?> validateHeaderFuture = scheduler.scheduleServiceTask(validateTask);
      final CompletableFuture<?> downloadBodiesFuture =
          scheduler.scheduleServiceTask(downloadBodiesTask);
      final CompletableFuture<PeerTaskResult<List<List<B>>>> validateBodiesFuture =
          scheduler.scheduleServiceTask(validateAndImportBodiesTask);

      // Hook in pipeline completion signaling.
      downloadTask.setLameDuckMode(true);
      downloadHeaderFuture.thenRun(() -> validateTask.setLameDuckMode(true));
      validateHeaderFuture.thenRun(() -> downloadBodiesTask.setLameDuckMode(true));
      downloadBodiesFuture.thenRun(() -> validateAndImportBodiesTask.setLameDuckMode(true));

      // Set the results when the pipeline is done.
      CompletableFuture.allOf(
              downloadHeaderFuture,
              validateHeaderFuture,
              downloadBodiesFuture,
              validateBodiesFuture)
          .whenComplete(
              (r, e) -> {
                if (e != null) {
                  result.get().completeExceptionally(e);
                } else {
                  try {
                    final List<B> importedBlocks =
                        validateBodiesFuture
                            .get()
                            .getResult()
                            .stream()
                            .flatMap(Collection::stream)
                            .collect(Collectors.toList());
                    result
                        .get()
                        .complete(new AbstractPeerTask.PeerTaskResult<>(peer, importedBlocks));
                  } catch (final InterruptedException | ExecutionException ex) {
                    result.get().completeExceptionally(ex);
                  }
                }
              });

    } else {
      LOG.warn("Import task requested with no checkpoint headers.");
    }
  }
}
