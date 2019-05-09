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
package tech.pegasys.pantheon.ethereum.worldstate;

import tech.pegasys.pantheon.ethereum.core.Hash;
import tech.pegasys.pantheon.ethereum.rlp.RLP;
import tech.pegasys.pantheon.ethereum.trie.MerklePatriciaTrie;
import tech.pegasys.pantheon.services.kvstore.KeyValueStorage;
import tech.pegasys.pantheon.services.kvstore.KeyValueStorage.Transaction;
import tech.pegasys.pantheon.util.bytes.Bytes32;
import tech.pegasys.pantheon.util.bytes.BytesValue;

import java.util.function.Function;

public class MarkSweepPruner {

  private static final BytesValue IN_USE = BytesValue.of(1);
  private static final int MARKS_PER_TRANSACTION = 100_000;
  private final WorldStateStorage worldStateStorage;
  private final KeyValueStorage markStorage;
  private Transaction markTransaction;
  private int transactionMarkCounter = 0;

  public MarkSweepPruner(
      final WorldStateStorage worldStateStorage, final KeyValueStorage markStorage) {
    this.worldStateStorage = worldStateStorage;
    this.markStorage = markStorage;
  }

  // Note chainHeadStateRoot must be the state root of the current chain head.
  // We can delay the actual sweep until a certain number of blocks in the future if we want to
  // have a certain number of block history available.
  public void markNode(
      final MerklePatriciaTrie<Bytes32, BytesValue> worldState,
      final Function<Hash, MerklePatriciaTrie<Bytes32, BytesValue>> getStorageTrie) {
    markTransaction = markStorage.startTransaction();
    worldState.visitAll(
        node -> {
          markNode(node.getHash());
          node.getValue().ifPresent(value -> processAccountState(getStorageTrie, value));
        });
    markTransaction.commit();
  }

  public void sweep() {
    worldStateStorage.prune(markStorage::mayContainKey);
  }

  private void processAccountState(
      final Function<Hash, MerklePatriciaTrie<Bytes32, BytesValue>> getStorageTrie,
      final BytesValue value) {
    final StateTrieAccountValue accountValue = StateTrieAccountValue.readFrom(RLP.input(value));
    markNode(accountValue.getCodeHash());

    getStorageTrie
        .apply(accountValue.getStorageRoot())
        .visitAll(storageNode -> markNode(storageNode.getHash()));
  }

  private void markNode(final Bytes32 hash) {
    transactionMarkCounter++;
    if (transactionMarkCounter > MARKS_PER_TRANSACTION) {
      markTransaction.commit();
      transactionMarkCounter = 0;
      markTransaction = markStorage.startTransaction();
    }
    markTransaction.put(hash, IN_USE);
  }
}
