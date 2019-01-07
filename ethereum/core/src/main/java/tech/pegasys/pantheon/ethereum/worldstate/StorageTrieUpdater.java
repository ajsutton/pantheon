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
import tech.pegasys.pantheon.util.bytes.Bytes32;
import tech.pegasys.pantheon.util.bytes.BytesValue;
import tech.pegasys.pantheon.util.uint.UInt256;

import java.util.Map;
import java.util.SortedMap;

public class StorageTrieUpdater {

  public static void updateStorageTrie(
      final MerklePatriciaTrie<Bytes32, BytesValue> storageTrie,
      final SortedMap<UInt256, UInt256> updatedStorage) {
    for (final Map.Entry<UInt256, UInt256> entry : updatedStorage.entrySet()) {
      final UInt256 value = entry.getValue();
      final Hash keyHash = Hash.hash(entry.getKey().getBytes());
      if (value.isZero()) {
        storageTrie.remove(keyHash);
      } else {
        storageTrie.put(keyHash, RLP.encode(out -> out.writeUInt256Scalar(entry.getValue())));
      }
    }
  }
}
