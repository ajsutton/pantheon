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
package tech.pegasys.pantheon.ethereum.core;

import tech.pegasys.pantheon.ethereum.rlp.RLPInput;
import tech.pegasys.pantheon.ethereum.rlp.RLPOutput;
import tech.pegasys.pantheon.util.bytes.BytesValue;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class BlockBody {

  private static final BlockBody EMPTY =
      new BlockBody(Collections.emptyList(), Collections.emptyList());

  private final List<Transaction> transactions;
  private final List<BlockHeader> ommers;
  private final Optional<BytesValue> rlp;

  public BlockBody(final List<Transaction> transactions, final List<BlockHeader> ommers) {
    this(transactions, ommers, Optional.empty());
  }

  private BlockBody(
      final List<Transaction> transactions,
      final List<BlockHeader> ommers,
      final Optional<BytesValue> rlp) {
    this.transactions = transactions;
    this.ommers = ommers;
    this.rlp = rlp;
  }

  public static BlockBody empty() {
    return EMPTY;
  }

  /** @return The list of transactions of the block. */
  public List<Transaction> getTransactions() {
    return transactions;
  }

  /** @return The list of ommers of the block. */
  public List<BlockHeader> getOmmers() {
    return ommers;
  }

  /**
   * Writes Block to {@link RLPOutput}.
   *
   * @param output Output to write to
   */
  public void writeTo(final RLPOutput output) {
    if (rlp.isPresent()) {
      output.writeRLPUnsafe(rlp.get());
      return;
    }
    output.startList();
    output.writeList(getTransactions(), Transaction::writeTo);
    output.writeList(getOmmers(), BlockHeader::writeTo);
    output.endList();
  }

  public static BlockBody readFrom(
      final RLPInput input, final BlockHeaderFunctions blockHeaderFunctions) {
    final BytesValue rawRlp = input.enterListAndReturnRlp();
    // TODO: Support multiple hard fork transaction formats.
    final BlockBody body =
        new BlockBody(
            input.readList(Transaction::readFrom),
            input.readList(rlp -> BlockHeader.readFrom(rlp, blockHeaderFunctions)),
            Optional.of(rawRlp));
    input.leaveList();
    return body;
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof BlockBody)) {
      return false;
    }
    final BlockBody other = (BlockBody) obj;
    return transactions.equals(other.transactions) && ommers.equals(other.ommers);
  }

  @Override
  public int hashCode() {
    return Objects.hash(transactions, ommers);
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append("BlockBody{");
    sb.append("transactions=").append(transactions).append(", ");
    sb.append("ommers=").append(ommers);
    return sb.append("}").toString();
  }
}
