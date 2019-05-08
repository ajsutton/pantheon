package tech.pegasys.pantheon.ethereum.worldstate;

import static org.assertj.core.api.Assertions.assertThat;

import tech.pegasys.pantheon.ethereum.core.BlockDataGenerator;
import tech.pegasys.pantheon.ethereum.core.Hash;
import tech.pegasys.pantheon.ethereum.core.MutableWorldState;
import tech.pegasys.pantheon.ethereum.storage.keyvalue.KeyValueStorageWorldStateStorage;
import tech.pegasys.pantheon.ethereum.trie.StoredMerklePatriciaTrie;
import tech.pegasys.pantheon.services.kvstore.InMemoryKeyValueStorage;

import java.util.function.Function;

import org.junit.Test;

public class MarkSweepPrunerTest {

  private final BlockDataGenerator gen = new BlockDataGenerator();

  @Test
  public void shouldMarkAllNodesInCurrentWorldState() {

    // Setup "remote" state
    final InMemoryKeyValueStorage markStorage = new InMemoryKeyValueStorage();
    final InMemoryKeyValueStorage stateStorage = new InMemoryKeyValueStorage();
    final WorldStateStorage worldStateStorage = new KeyValueStorageWorldStateStorage(stateStorage);
    final WorldStateArchive worldStateArchive = new WorldStateArchive(worldStateStorage);
    final MutableWorldState worldState = worldStateArchive.getMutable();

    // Generate accounts and save corresponding state root
    gen.createRandomContractAccountsWithNonEmptyStorage(worldState, 20000);
    final Hash stateRoot = worldState.rootHash();

    final MarkSweepPruner pruner = new MarkSweepPruner(worldStateStorage, markStorage);
    pruner.markNode(
        new StoredMerklePatriciaTrie<>(
            worldStateStorage::getAccountStateTrieNode,
            stateRoot,
            Function.identity(),
            Function.identity()),
        rootHash ->
            new StoredMerklePatriciaTrie<>(
                worldStateStorage::getAccountStorageTrieNode,
                rootHash,
                Function.identity(),
                Function.identity()));

    assertThat(markStorage.keySet()).containsExactlyInAnyOrderElementsOf(stateStorage.keySet());
  }
}
