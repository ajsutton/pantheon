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

import static java.util.Arrays.asList;

import tech.pegasys.pantheon.ethereum.chain.BlockchainStorage;
import tech.pegasys.pantheon.ethereum.mainnet.ProtocolSchedule;
import tech.pegasys.pantheon.ethereum.mainnet.ScheduleBasedBlockHashFunction;
import tech.pegasys.pantheon.ethereum.privacy.PrivateKeyValueStorage;
import tech.pegasys.pantheon.ethereum.privacy.PrivateStateKeyValueStorage;
import tech.pegasys.pantheon.ethereum.privacy.PrivateStateStorage;
import tech.pegasys.pantheon.ethereum.privacy.PrivateTransactionStorage;
import tech.pegasys.pantheon.ethereum.storage.StorageProvider;
import tech.pegasys.pantheon.ethereum.worldstate.WorldStateStorage;
import tech.pegasys.pantheon.metrics.MetricsSystem;
import tech.pegasys.pantheon.services.kvstore.ColumnarRocksDbKeyValueStorage;
import tech.pegasys.pantheon.services.kvstore.KeyValueStorage;
import tech.pegasys.pantheon.services.kvstore.RocksDbConfiguration;
import tech.pegasys.pantheon.services.kvstore.RocksDbKeyValueStorage;
import tech.pegasys.pantheon.services.kvstore.SegmentedKeyValueStorage;
import tech.pegasys.pantheon.services.kvstore.SegmentedKeyValueStorage.Segment;
import tech.pegasys.pantheon.util.bytes.BytesValue;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;

public class RocksDbStorageProvider {

  public static StorageProvider create(
      final RocksDbConfiguration rocksDbConfiguration, final MetricsSystem metricsSystem)
      throws IOException {
    if (rocksDbConfiguration.useColumns()) {
      return createSegmentedProvider(rocksDbConfiguration, metricsSystem);
    } else {
      return createUnsegmentedProvider(rocksDbConfiguration, metricsSystem);
    }
  }

  private static StorageProvider createUnsegmentedProvider(
      final RocksDbConfiguration rocksDbConfiguration, final MetricsSystem metricsSystem)
      throws IOException {
    Files.createDirectories(rocksDbConfiguration.getDatabaseDir());
    final KeyValueStorage kv = RocksDbKeyValueStorage.create(rocksDbConfiguration, metricsSystem);
    return new KeyValueStorageProvider(kv);
  }

  public static StorageProvider createSegmentedProvider(
      final RocksDbConfiguration rocksDbConfiguration, final MetricsSystem metricsSystem)
      throws IOException {
    Files.createDirectories(rocksDbConfiguration.getDatabaseDir());
    final SegmentedKeyValueStorage<?> columnarStorage =
        ColumnarRocksDbKeyValueStorage.create(
            rocksDbConfiguration,
            asList(RocksDbSegment.BLOCKCHAIN, RocksDbSegment.WORLD_STATE),
            metricsSystem);
    return new StorageProvider() {
      @Override
      public BlockchainStorage createBlockchainStorage(final ProtocolSchedule<?> protocolSchedule) {
        return new KeyValueStoragePrefixedKeyBlockchainStorage(
            storageAdapter(RocksDbSegment.BLOCKCHAIN),
            ScheduleBasedBlockHashFunction.create(protocolSchedule));
      }

      @Override
      public WorldStateStorage createWorldStateStorage() {
        return new KeyValueStorageWorldStateStorage(storageAdapter(RocksDbSegment.WORLD_STATE));
      }

      @Override
      public PrivateTransactionStorage createPrivateTransactionStorage() {
        return new PrivateKeyValueStorage(storageAdapter(RocksDbSegment.PRIVATE_STATE));
      }

      @Override
      public PrivateStateStorage createPrivateStateStorage() {
        return new PrivateStateKeyValueStorage(storageAdapter(RocksDbSegment.PRIVATE_STATE));
      }

      @Override
      public void close() throws IOException {
        columnarStorage.close();
      }

      private KeyValueStorage storageAdapter(final RocksDbSegment segment) {
        return new SegmentedKeyValueStorageAdapter<>(segment, columnarStorage);
      }
    };
  }

  private static class SegmentedKeyValueStorageAdapter<S> implements KeyValueStorage {
    private final S segmentHandle;
    private final SegmentedKeyValueStorage<S> storage;

    private SegmentedKeyValueStorageAdapter(
        final Segment segment, final SegmentedKeyValueStorage<S> storage) {
      this.segmentHandle = storage.getSegmentIdentifierByName(segment);
      this.storage = storage;
    }

    @Override
    public Optional<BytesValue> get(final BytesValue key) throws StorageException {
      return storage.get(segmentHandle, key);
    }

    @Override
    public Transaction startTransaction() throws StorageException {
      final SegmentedKeyValueStorage.Transaction<S> transaction = storage.startTransaction();
      return new Transaction() {
        @Override
        public void put(final BytesValue key, final BytesValue value) {
          transaction.put(segmentHandle, key, value);
        }

        @Override
        public void remove(final BytesValue key) {
          transaction.remove(segmentHandle, key);
        }

        @Override
        public void commit() throws StorageException {
          transaction.commit();
        }

        @Override
        public void rollback() {
          transaction.rollback();
        }
      };
    }

    @Override
    public void close() throws IOException {
      storage.close();
    }
  }

  private enum RocksDbSegment implements Segment {
    BLOCKCHAIN(new byte[] {1}),
    WORLD_STATE(new byte[] {2}),
    PRIVATE_STATE(new byte[] {3});

    private final byte[] id;

    RocksDbSegment(final byte[] id) {
      this.id = id;
    }

    @Override
    public String getName() {
      return name();
    }

    @Override
    public byte[] getId() {
      return id;
    }
  }
}
