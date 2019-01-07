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
package tech.pegasys.pantheon.ethereum.worldstate;

import tech.pegasys.pantheon.ethereum.core.Account;
import tech.pegasys.pantheon.ethereum.core.Address;
import tech.pegasys.pantheon.ethereum.core.Hash;
import tech.pegasys.pantheon.ethereum.core.HashStub;
import tech.pegasys.pantheon.ethereum.core.MutableAccount;
import tech.pegasys.pantheon.ethereum.core.WorldState;
import tech.pegasys.pantheon.ethereum.core.WorldUpdater;
import tech.pegasys.pantheon.ethereum.mainnet.account.AccountInit;
import tech.pegasys.pantheon.ethereum.storage.keyvalue.KeyValueStorageWorldStateStorage;
import tech.pegasys.pantheon.services.kvstore.InMemoryKeyValueStorage;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

/**
 * A simple extension of {@link DefaultMutableWorldState} that tracks in memory the mapping of hash
 * to address for its accounts for debugging purposes. It also provides a full toString() method
 * that display the content of the world state. It is obviously only mean for testing or debugging.
 */
public class DebuggableMutableWorldState extends DefaultMutableWorldState {

  // TODO: This is more complex than it should due to DefaultMutableWorldState.accounts() not being
  // implmemented (pending NC-746). Once that is fixed, we won't need to keep the set of account
  // hashes at all, just the hashtoAddress map (this is also why things are separated this way,
  // it will make it easier to update later).

  private static class DebugInfo {
    private final Set<Address> accounts = new HashSet<>();
    private final Set<Address> stubs = new HashSet<>();

    private void addAll(final DebugInfo other) {
      this.accounts.addAll(other.accounts);
      this.stubs.addAll(other.stubs);
    }
  }

  private final DebugInfo info = new DebugInfo();

  public DebuggableMutableWorldState() {
    super(new KeyValueStorageWorldStateStorage(new InMemoryKeyValueStorage()));
  }

  public DebuggableMutableWorldState(final WorldState worldState) {
    super(worldState);

    if (worldState instanceof DebuggableMutableWorldState) {
      final DebuggableMutableWorldState dws = ((DebuggableMutableWorldState) worldState);
      info.addAll(dws.info);
    } else {
      // TODO: on NC-746 gets in, we can remove this. That is, post NC-746, we won't be relying
      // on info.accounts to know that accounts exists, so the only thing we will not have in
      // this branch is info.addressToHash, but that's not a huge deal.
      throw new RuntimeException(worldState + " is not a debuggable word state");
    }
  }

  @Override
  public WorldUpdater updater() {
    return new InfoCollectingUpdater(super.updater(), info);
  }

  @Override
  public Stream<Account> accounts() {
    return info.accounts.stream().map(this::get).filter(Objects::nonNull);
  }

  @Override
  public String toString() {
    final StringBuilder builder = new StringBuilder();
    builder.append(rootHash()).append(":\n");
    accounts()
        .forEach(
            account -> {
              final Address address = account.getAddress();
              builder
                  .append("  ")
                  .append(address == null ? "<unknown>" : address)
                  .append(" [")
                  .append(account.getAddressHash())
                  .append("]:\n");
              builder.append("    nonce: ").append(account.getNonce()).append('\n');
              builder.append("    balance: ").append(account.getBalance()).append('\n');
              builder.append("    code: ").append(account.getCode()).append('\n');
            });
    info.stubs.forEach(
        address -> {
          final HashStub hashStub = getHashStub(address);
          if (hashStub == null) {
            return;
          }
          builder
              .append("  ")
              .append(address)
              .append(" [")
              .append(Hash.hash(address))
              .append("] (Evicted):\n");

          builder.append("    storageRoot: ").append(hashStub.getStorageRoot()).append('\n');
          builder.append("    codeHash: ").append(hashStub.getCodeHash()).append('\n');
        });
    return builder.toString();
  }

  private class InfoCollectingUpdater implements WorldUpdater {
    private final WorldUpdater wrapped;
    private final DebugInfo commitInfo;
    private DebugInfo ownInfo = new DebugInfo();

    InfoCollectingUpdater(final WorldUpdater wrapped, final DebugInfo info) {
      this.wrapped = wrapped;
      this.commitInfo = info;
    }

    private void record(final Address address) {
      ownInfo.accounts.add(address);
    }

    @Override
    public MutableAccount createAccount(
        final Address address, final AccountInit accountInit, final long currentBlockNumber) {
      record(address);
      return wrapped.createAccount(address, accountInit, currentBlockNumber);
    }

    @Override
    public MutableAccount getOrCreate(
        final Address address, final AccountInit accountInit, final long currentBlockNumber) {
      record(address);
      return wrapped.getOrCreate(address, accountInit, currentBlockNumber);
    }

    @Override
    public MutableAccount getMutable(final Address address) {
      record(address);
      return wrapped.getMutable(address);
    }

    @Override
    public void deleteAccount(final Address address) {
      wrapped.deleteAccount(address);
    }

    @Override
    public void evictAccount(final Address address) {
      wrapped.evictAccount(address);
    }

    @Override
    public Collection<MutableAccount> getTouchedAccounts() {
      return wrapped.getTouchedAccounts();
    }

    @Override
    public void revert() {
      ownInfo = new DebugInfo();
      wrapped.revert();
    }

    @Override
    public void commit() {
      commitInfo.addAll(ownInfo);
      wrapped.commit();
    }

    @Override
    public WorldUpdater updater() {
      return new InfoCollectingUpdater(wrapped.updater(), ownInfo);
    }

    @Override
    public Account get(final Address address) {
      record(address);
      return wrapped.get(address);
    }

    @Override
    public HashStub getHashStub(final Address address) {
      ownInfo.stubs.add(address);
      return wrapped.getHashStub(address);
    }
  }
}
