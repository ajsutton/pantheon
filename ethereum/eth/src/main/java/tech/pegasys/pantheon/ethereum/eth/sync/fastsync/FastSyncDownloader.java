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

import static tech.pegasys.pantheon.ethereum.eth.sync.fastsync.FastSyncResult.FAST_SYNC_UNAVAILABLE;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FastSyncDownloader<C> {
  private static final Logger LOG = LogManager.getLogger();
  private final FastSyncActions<C> fastSyncActions;

  public FastSyncDownloader(final FastSyncActions<C> fastSyncActions) {
    this.fastSyncActions = fastSyncActions;
  }

  public CompletableFuture<FastSyncResult> start() {
    LOG.info("Fast sync enabled");
    return fastSyncActions
        .waitForSuitablePeers()
        .thenApply(ifSuccessful(fastSyncActions::selectPivotBlock))
        .thenApply(ifSuccessful(() -> FastSyncState.withResult(FAST_SYNC_UNAVAILABLE)))
        .thenApply(FastSyncState::getLastActionResult);
  }

  private FastSyncState ifSuccessful(
      final FastSyncState state, final UnaryOperator<FastSyncState> nextAction) {
    return state.getLastActionResult() == FastSyncResult.SUCCESS ? nextAction.apply(state) : state;
  }

  private Function<FastSyncState, FastSyncState> ifSuccessful(
      final Supplier<FastSyncState> nextAction) {
    return state -> ifSuccessful(state, ignored -> nextAction.get());
  }
}
