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

import java.util.Objects;
import java.util.OptionalLong;

public class FastSyncState {

  private final FastSyncResult lastActionResult;
  private final OptionalLong pivotBlockNumber;

  public FastSyncState(final FastSyncResult lastActionResult, final OptionalLong pivotBlockNumber) {
    this.lastActionResult = lastActionResult;
    this.pivotBlockNumber = pivotBlockNumber;
  }

  public static FastSyncState withResult(final FastSyncResult result) {
    return new FastSyncState(result, OptionalLong.empty());
  }

  public FastSyncResult getLastActionResult() {
    return lastActionResult;
  }

  public OptionalLong getPivotBlockNumber() {
    return pivotBlockNumber;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final FastSyncState that = (FastSyncState) o;
    return lastActionResult == that.lastActionResult
        && Objects.equals(pivotBlockNumber, that.pivotBlockNumber);
  }

  @Override
  public int hashCode() {
    return Objects.hash(lastActionResult, pivotBlockNumber);
  }
}
