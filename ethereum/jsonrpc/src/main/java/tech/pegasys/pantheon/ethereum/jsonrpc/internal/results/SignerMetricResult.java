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
package tech.pegasys.pantheon.ethereum.jsonrpc.internal.results;

import tech.pegasys.pantheon.ethereum.core.Address;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"address", "proposedBlockCount", "lastProposedBlockNumber"})
public class SignerMetricResult {

  private final String address;
  private long proposedBlockCount;
  private long lastProposedBlockNumber;

  public SignerMetricResult(final Address address) {
    this.address = address.toString();
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final SignerMetricResult that = (SignerMetricResult) o;
    return proposedBlockCount == that.proposedBlockCount
        && lastProposedBlockNumber == that.lastProposedBlockNumber
        && Objects.equals(address, that.address);
  }

  @Override
  public int hashCode() {
    return Objects.hash(address, proposedBlockCount, lastProposedBlockNumber);
  }

  @JsonGetter(value = "address")
  public String getAddress() {
    return address;
  }

  @JsonGetter(value = "proposedBlockCount")
  public String getProposedBlockCount() {
    return Quantity.create(proposedBlockCount);
  }

  @JsonGetter(value = "lastProposedBlockNumber")
  public String getLastProposedBlockNumber() {
    return Quantity.create(lastProposedBlockNumber);
  }

  public void incrementeNbBlock() {
    this.proposedBlockCount++;
  }

  public void setLastProposedBlockNumber(final long lastProposedBlockNumber) {
    this.lastProposedBlockNumber = lastProposedBlockNumber;
  }
}
