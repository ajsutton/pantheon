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
import static org.mockito.Mockito.when;

import java.util.concurrent.CompletableFuture;

import org.junit.Test;

public class FastSyncDownloaderTest {

  @SuppressWarnings("unchecked")
  private final FastSyncActions<Void> fastSyncActions = mock(FastSyncActions.class);

  private final FastSyncDownloader<Void> downloader = new FastSyncDownloader<>(fastSyncActions);

  @Test
  public void shouldWaitForSuitablePeersThenFail() {
    when(fastSyncActions.waitForSuitablePeers())
        .thenReturn(completedFuture(FastSyncResult.SUCCESS));

    final CompletableFuture<FastSyncResult> result = downloader.start();

    assertThat(result).isCompletedWithValue(FastSyncResult.FAST_SYNC_UNAVAILABLE);
  }

  @Test
  public void shouldAbortIfWaitForSuitablePeersFails() {
    when(fastSyncActions.waitForSuitablePeers())
        .thenReturn(completedFuture(FastSyncResult.UNEXPECTED_ERROR));

    final CompletableFuture<FastSyncResult> result = downloader.start();

    assertThat(result).isCompletedWithValue(FastSyncResult.UNEXPECTED_ERROR);
  }
}
