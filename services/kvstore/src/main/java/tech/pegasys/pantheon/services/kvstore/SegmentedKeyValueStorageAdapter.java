package tech.pegasys.pantheon.services.kvstore;

import tech.pegasys.pantheon.services.kvstore.SegmentedKeyValueStorage.Segment;
import tech.pegasys.pantheon.util.bytes.BytesValue;

import java.io.IOException;
import java.util.Optional;

public class SegmentedKeyValueStorageAdapter<S> implements KeyValueStorage {
  private final S segmentHandle;
  private final SegmentedKeyValueStorage<S> storage;

  public SegmentedKeyValueStorageAdapter(
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
