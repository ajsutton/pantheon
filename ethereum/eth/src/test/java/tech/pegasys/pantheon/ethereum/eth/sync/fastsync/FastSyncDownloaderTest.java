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
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static tech.pegasys.pantheon.ethereum.eth.sync.fastsync.FastSyncError.CHAIN_TOO_SHORT;
import static tech.pegasys.pantheon.ethereum.eth.sync.fastsync.FastSyncError.UNEXPECTED_ERROR;

import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.CompletableFuture;

import org.junit.Test;

public class FastSyncDownloaderTest {

  @SuppressWarnings("unchecked")
  private final FastSyncActions<Void> fastSyncActions = mock(FastSyncActions.class);

  private final FastSyncDownloader<Void> downloader = new FastSyncDownloader<>(fastSyncActions);

  @Test
  public void shouldCompleteFastSyncSuccessfully() {
    when(fastSyncActions.waitForSuitablePeers()).thenReturn(completedFuture(null));
    final FastSyncState selectPivotBlockState = new FastSyncState(OptionalLong.of(50));
    when(fastSyncActions.selectPivotBlock()).thenReturn(selectPivotBlockState);
    when(fastSyncActions.downloadPivotBlockHeader(selectPivotBlockState))
        .thenReturn(completedFuture(selectPivotBlockState));

    final CompletableFuture<Optional<FastSyncError>> result = downloader.start();

    verify(fastSyncActions).waitForSuitablePeers();
    verify(fastSyncActions).selectPivotBlock();
    verify(fastSyncActions).downloadPivotBlockHeader(selectPivotBlockState);
    verifyNoMoreInteractions(fastSyncActions);
    assertThat(result).isCompletedWithValue(Optional.of(FastSyncError.FAST_SYNC_UNAVAILABLE));
  }

  @Test
  public void shouldAbortIfWaitForSuitablePeersFails() {
    when(fastSyncActions.waitForSuitablePeers())
        .thenReturn(completedExceptionally(new FastSyncException(UNEXPECTED_ERROR)));

    final CompletableFuture<Optional<FastSyncError>> result = downloader.start();

    assertThat(result).isCompletedWithValue(Optional.of(UNEXPECTED_ERROR));

    verify(fastSyncActions).waitForSuitablePeers();
    verifyNoMoreInteractions(fastSyncActions);
  }

  @Test
  public void shouldAbortIfSelectPivotBlockFails() {
    when(fastSyncActions.waitForSuitablePeers()).thenReturn(completedFuture(null));
    when(fastSyncActions.selectPivotBlock()).thenThrow(new FastSyncException(CHAIN_TOO_SHORT));

    final CompletableFuture<Optional<FastSyncError>> result = downloader.start();

    assertThat(result).isCompletedWithValue(Optional.of(CHAIN_TOO_SHORT));

    verify(fastSyncActions).waitForSuitablePeers();
    verify(fastSyncActions).selectPivotBlock();
    verifyNoMoreInteractions(fastSyncActions);
  }

  private <T> CompletableFuture<T> completedExceptionally(final Throwable error) {
    final CompletableFuture<T> result = new CompletableFuture<>();
    result.completeExceptionally(error);
    return result;
  }
}
