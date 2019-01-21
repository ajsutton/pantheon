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
package tech.pegasys.pantheon.ethereum.eth.sync.tasks;

import tech.pegasys.pantheon.ethereum.core.BlockHeader;
import tech.pegasys.pantheon.ethereum.eth.manager.AbstractRetryingPeerTask;
import tech.pegasys.pantheon.ethereum.eth.manager.EthContext;
import tech.pegasys.pantheon.ethereum.eth.manager.exceptions.NoAvailablePeersException;
import tech.pegasys.pantheon.ethereum.eth.manager.exceptions.PeerBreachedProtocolException;
import tech.pegasys.pantheon.ethereum.eth.manager.exceptions.PeerDisconnectedException;
import tech.pegasys.pantheon.ethereum.mainnet.ProtocolSchedule;
import tech.pegasys.pantheon.metrics.LabelledMetric;
import tech.pegasys.pantheon.metrics.OperationTimer;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

public class GetPivotBlockHeaderTask extends AbstractRetryingPeerTask<List<BlockHeader>> {

  private static final int MAX_PIVOT_BLOCK_RETRIES = 5;
  private final ProtocolSchedule<?> protocolSchedule;
  private final EthContext ethContext;
  private final LabelledMetric<OperationTimer> ethTasksTimer;
  private final long pivotBlockNumber;

  public GetPivotBlockHeaderTask(
      final ProtocolSchedule<?> protocolSchedule,
      final EthContext ethContext,
      final LabelledMetric<OperationTimer> ethTasksTimer,
      final long pivotBlockNumber) {
    super(ethContext, MAX_PIVOT_BLOCK_RETRIES, ethTasksTimer);
    this.protocolSchedule = protocolSchedule;
    this.ethContext = ethContext;
    this.ethTasksTimer = ethTasksTimer;
    this.pivotBlockNumber = pivotBlockNumber;
  }

  @Override
  protected CompletableFuture<List<BlockHeader>> executePeerTask() {
    final AbstractGetHeadersFromPeerTask getHeadersTask =
        GetHeadersFromPeerByNumberTask.forSingleNumber(
            protocolSchedule, ethContext, pivotBlockNumber, ethTasksTimer);
    return executeSubTask(getHeadersTask::run)
        .thenApply(
            peerResult -> {
              if (!peerResult.getResult().isEmpty()) {
                result.get().complete(peerResult.getResult());
              }
              return peerResult.getResult();
            });
  }

  public CompletableFuture<BlockHeader> getPivotBlockHeader() {
    return run().thenApply(singletonList -> singletonList.get(0));
  }

  @Override
  protected boolean isRetryableError(final Throwable error) {
    return error instanceof NoAvailablePeersException
        || error instanceof TimeoutException
        || error instanceof PeerBreachedProtocolException
        || error instanceof PeerDisconnectedException;
  }
}
