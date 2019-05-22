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
package tech.pegasys.pantheon.ethereum.storage.keyvalue;

import tech.pegasys.pantheon.ethereum.chain.BlockchainStorage;
import tech.pegasys.pantheon.ethereum.mainnet.ProtocolSchedule;
import tech.pegasys.pantheon.ethereum.mainnet.ScheduleBasedBlockHeaderFunctions;
import tech.pegasys.pantheon.ethereum.privacy.PrivateKeyValueStorage;
import tech.pegasys.pantheon.ethereum.privacy.PrivateStateKeyValueStorage;
import tech.pegasys.pantheon.ethereum.privacy.PrivateStateStorage;
import tech.pegasys.pantheon.ethereum.privacy.PrivateTransactionStorage;
import tech.pegasys.pantheon.ethereum.storage.StorageProvider;
import tech.pegasys.pantheon.ethereum.worldstate.WorldStateStorage;
import tech.pegasys.pantheon.services.kvstore.KeyValueStorage;

import java.io.IOException;

public class KeyValueStorageProvider implements StorageProvider {

  private final KeyValueStorage blockchainStorage;
  private final KeyValueStorage worldStateStorage;
  private final KeyValueStorage privateTransactionStorage;
  private final KeyValueStorage privateStateStorage;
  private final KeyValueStorage pruningStorage;

  public KeyValueStorageProvider(final KeyValueStorage keyValueStorage) {
    this(keyValueStorage, keyValueStorage, keyValueStorage, keyValueStorage, keyValueStorage);
  }

  public KeyValueStorageProvider(
      final KeyValueStorage blockchainStorage,
      final KeyValueStorage worldStateStorage,
      final KeyValueStorage privateTransactionStorage,
      final KeyValueStorage privateStateStorage,
      final KeyValueStorage pruningStorage) {
    this.blockchainStorage = blockchainStorage;
    this.worldStateStorage = worldStateStorage;
    this.privateTransactionStorage = privateTransactionStorage;
    this.privateStateStorage = privateStateStorage;
    this.pruningStorage = pruningStorage;
  }

  @Override
  public BlockchainStorage createBlockchainStorage(final ProtocolSchedule<?> protocolSchedule) {
    return new KeyValueStoragePrefixedKeyBlockchainStorage(
        blockchainStorage, ScheduleBasedBlockHeaderFunctions.create(protocolSchedule));
  }

  @Override
  public WorldStateStorage createWorldStateStorage() {
    return new KeyValueStorageWorldStateStorage(worldStateStorage);
  }

  @Override
  public PrivateTransactionStorage createPrivateTransactionStorage() {
    return new PrivateKeyValueStorage(privateTransactionStorage);
  }

  @Override
  public PrivateStateStorage createPrivateStateStorage() {
    return new PrivateStateKeyValueStorage(privateStateStorage);
  }

  @Override
  public KeyValueStorage createPruningStorage() {
    return pruningStorage;
  }

  @Override
  public void close() throws IOException {
    blockchainStorage.close();
    worldStateStorage.close();
    privateTransactionStorage.close();
    privateStateStorage.close();
    pruningStorage.close();
  }
}
