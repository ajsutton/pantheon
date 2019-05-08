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

import tech.pegasys.pantheon.ethereum.storage.StorageProvider;
import tech.pegasys.pantheon.metrics.MetricsSystem;
import tech.pegasys.pantheon.services.kvstore.ColumnarRocksDbKeyValueStorage;
import tech.pegasys.pantheon.services.kvstore.KeyValueStorage;
import tech.pegasys.pantheon.services.kvstore.RocksDbConfiguration;
import tech.pegasys.pantheon.services.kvstore.RocksDbKeyValueStorage;
import tech.pegasys.pantheon.services.kvstore.SegmentedKeyValueStorage;
import tech.pegasys.pantheon.services.kvstore.SegmentedKeyValueStorage.Segment;
import tech.pegasys.pantheon.services.kvstore.SegmentedKeyValueStorageAdapter;

import java.io.IOException;
import java.nio.file.Files;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RocksDbStorageProvider {
  private static final Logger LOG = LogManager.getLogger();

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
    LOG.info("Using RocksDB colunns");
    Files.createDirectories(rocksDbConfiguration.getDatabaseDir());
    final SegmentedKeyValueStorage<?> columnarStorage =
        ColumnarRocksDbKeyValueStorage.create(
            rocksDbConfiguration,
            asList(
                RocksDbSegment.BLOCKCHAIN,
                RocksDbSegment.WORLD_STATE,
                RocksDbSegment.PRIVATE_STATE),
            metricsSystem);

    return new KeyValueStorageProvider(
        new SegmentedKeyValueStorageAdapter<>(RocksDbSegment.BLOCKCHAIN, columnarStorage),
        new SegmentedKeyValueStorageAdapter<>(RocksDbSegment.WORLD_STATE, columnarStorage),
        new SegmentedKeyValueStorageAdapter<>(RocksDbSegment.PRIVATE_TRANSACTIONS, columnarStorage),
        new SegmentedKeyValueStorageAdapter<>(RocksDbSegment.PRIVATE_STATE, columnarStorage));
  }

  private enum RocksDbSegment implements Segment {
    BLOCKCHAIN(new byte[] {1}),
    WORLD_STATE(new byte[] {2}),
    PRIVATE_TRANSACTIONS(new byte[] {3}),
    PRIVATE_STATE(new byte[] {4});

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
