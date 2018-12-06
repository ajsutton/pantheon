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

import static org.assertj.core.api.Assertions.assertThat;

import tech.pegasys.pantheon.ethereum.core.Account;
import tech.pegasys.pantheon.ethereum.core.Address;
import tech.pegasys.pantheon.ethereum.core.AddressHelpers;
import tech.pegasys.pantheon.ethereum.core.Hash;
import tech.pegasys.pantheon.ethereum.core.Wei;
import tech.pegasys.pantheon.util.bytes.Bytes32;
import tech.pegasys.pantheon.util.bytes.BytesValue;
import tech.pegasys.pantheon.util.uint.UInt256;

import java.math.BigInteger;
import java.util.NavigableMap;

import org.junit.Test;

public class AccountSerializerTest {
  private final AccountSerializer<TestAccountState> serializer =
      new AccountSerializer<>(TestAccountState::new);

  @Test
  public void shouldRoundTripAccountWithRentBlock() {
    final TestAccountState input =
        new TestAccountState(
            AddressHelpers.ofValue(424824),
            42424,
            Wei.of(99283),
            BigInteger.valueOf(11313),
            1993,
            null,
            Hash.EMPTY_TRIE_HASH,
            Hash.EMPTY);

    final BytesValue serialized = input.serialize(serializer);

    final TestAccountState output =
        serializer.deserializeAccount(input.address, input.addressHash, serialized);

    assertThat(output).isEqualToComparingFieldByField(input);
  }

  @Test
  public void shouldRoundTripAccountWithoutRentBlock() {
    final TestAccountState input =
        new TestAccountState(
            AddressHelpers.ofValue(424824),
            42424,
            Wei.of(99283),
            BigInteger.ZERO,
            Account.NO_RENT_BLOCK,
            null,
            Hash.EMPTY_TRIE_HASH,
            Hash.EMPTY);

    final BytesValue serialized = input.serialize(serializer);

    final TestAccountState output =
        serializer.deserializeAccount(input.address, input.addressHash, serialized);

    assertThat(output).isEqualToComparingFieldByField(input);
  }

  @Test
  public void shouldRoundTripAccountWithStorageSizeButNoRentBlock() {
    final TestAccountState input =
        new TestAccountState(
            AddressHelpers.ofValue(424824),
            42424,
            Wei.of(99283),
            BigInteger.ZERO,
            Account.NO_RENT_BLOCK,
            BigInteger.valueOf(103),
            Hash.EMPTY_TRIE_HASH,
            Hash.EMPTY);
    final TestAccountState expected =
        new TestAccountState(
            AddressHelpers.ofValue(424824),
            42424,
            Wei.of(99283),
            BigInteger.ZERO,
            0,
            BigInteger.valueOf(103),
            Hash.EMPTY_TRIE_HASH,
            Hash.EMPTY);

    final BytesValue serialized = input.serialize(serializer);

    final TestAccountState output =
        serializer.deserializeAccount(input.address, input.addressHash, serialized);

    assertThat(output).isEqualToComparingFieldByField(expected);
  }

  @Test
  public void shouldUseExpectedNumberOfBytesForEmptyAccount() {
    final TestAccountState emptyAccount =
        new TestAccountState(
            Address.ECREC,
            0,
            Wei.ZERO,
            BigInteger.ZERO,
            0,
            BigInteger.ZERO,
            Hash.EMPTY_TRIE_HASH,
            Hash.EMPTY);
    final BytesValue result = emptyAccount.serialize(serializer);
    assertThat(result.size()).isEqualTo(Account.EMPTY_ACCOUNT_STORAGE_SIZE.intValue());
  }

  private static class TestAccountState implements Account {
    private final Address address;
    private final Hash addressHash;
    private final long nonce;
    private final Wei balance;
    private final BigInteger rentBalance;
    private final long rentBlock;
    private final Hash storageRoot;
    private final Hash codeHash;
    private final BigInteger storageSize;

    private TestAccountState(
        final Address address,
        final long nonce,
        final Wei balance,
        final BigInteger rentBalance,
        final long rentBlock,
        final BigInteger storageSize,
        final Hash storageRoot,
        final Hash codeHash) {
      this(
          address,
          Hash.hash(address),
          nonce,
          balance,
          rentBalance,
          rentBlock,
          storageSize,
          storageRoot,
          codeHash);
    }

    private TestAccountState(
        final Address address,
        final Hash addressHash,
        final long nonce,
        final Wei balance,
        final BigInteger rentBalance,
        final long rentBlock,
        final BigInteger storageSize,
        final Hash storageRoot,
        final Hash codeHash) {
      this.address = address;
      this.addressHash = addressHash;
      this.nonce = nonce;
      this.balance = balance;
      this.rentBalance = rentBalance;
      this.rentBlock = rentBlock;
      this.storageSize = storageSize;
      this.storageRoot = storageRoot;
      this.codeHash = codeHash;
    }

    @Override
    public Address getAddress() {
      return address;
    }

    @Override
    public Hash getAddressHash() {
      return addressHash;
    }

    @Override
    public long getNonce() {
      return nonce;
    }

    @Override
    public Wei getBalance() {
      return balance;
    }

    @Override
    public BigInteger getRentBalance() {
      return rentBalance;
    }

    @Override
    public long getRentBlock() {
      return rentBlock;
    }

    @Override
    public Hash getCodeHash() {
      return codeHash;
    }

    @Override
    public UInt256 getStorageValue(final UInt256 key) {
      throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public UInt256 getOriginalStorageValue(final UInt256 key) {
      throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public NavigableMap<Bytes32, UInt256> storageEntriesFrom(
        final Bytes32 startKeyHash, final int limit) {
      return null;
    }

    @Override
    public BigInteger getStorageSize() {
      return storageSize;
    }

    @Override
    public BigInteger getOriginalStorageSize() {
      throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public BytesValue getCode() {
      throw new UnsupportedOperationException("Not implemented");
    }

    public BytesValue serialize(final AccountSerializer<?> serializer) {
      return serializer.serializeAccount(this, codeHash, storageRoot);
    }
  }
}
