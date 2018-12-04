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

import tech.pegasys.pantheon.util.bytes.Bytes32;
import tech.pegasys.pantheon.util.bytes.BytesValue;
import tech.pegasys.pantheon.util.uint.UInt256;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;

public class StubAccount implements MutableAccount {

  private long nonce = 0;
  private Wei balance = Wei.ZERO;
  private BigInteger rentBalance = BigInteger.ZERO;
  private long rentBlock = NO_RENT_BLOCK;
  private BigInteger storageSize = BigInteger.ZERO;
  private BytesValue code = BytesValue.EMPTY;
  private final Map<UInt256, UInt256> storage = new HashMap<>();
  private final Address address;

  public StubAccount() {
    this(AddressHelpers.ofValue(0));
  }

  public StubAccount(final Address address) {
    this.address = address;
  }

  @Override
  public Address getAddress() {
    return address;
  }

  @Override
  public long getNonce() {
    return nonce;
  }

  @Override
  public void setNonce(final long nonce) {
    this.nonce = nonce;
  }

  @Override
  public Wei getBalance() {
    return balance;
  }

  @Override
  public void setBalance(final Wei balance) {
    this.balance = balance;
  }

  @Override
  public BigInteger getRentBalance() {
    return rentBalance;
  }

  @Override
  public void setRentBalance(final BigInteger rentBalance) {
    this.rentBalance = rentBalance;
  }

  @Override
  public long getRentBlock() {
    return rentBlock;
  }

  @Override
  public BigInteger getStorageSize() {
    return storageSize;
  }

  @Override
  public void adjustStorageSize(final int adjustmentAmount) {
    storageSize = storageSize.add(BigInteger.valueOf(adjustmentAmount));
  }

  @Override
  public void setStorageSize(final BigInteger storageSize) {
    this.storageSize = storageSize;
  }

  @Override
  public void setRentBlock(final long rentBlock) {
    this.rentBlock = rentBlock;
  }

  @Override
  public BytesValue getCode() {
    return code;
  }

  @Override
  public Hash getCodeHash() {
    return code.isEmpty() ? Hash.EMPTY : Hash.hash(code);
  }

  @Override
  public UInt256 getStorageValue(final UInt256 key) {
    return storage.getOrDefault(key, UInt256.ZERO);
  }

  @Override
  public UInt256 getOriginalStorageValue(final UInt256 key) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public NavigableMap<Bytes32, UInt256> storageEntriesFrom(
      final Bytes32 startKeyHash, final int limit) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public void setCode(final BytesValue code) {
    this.code = code;
  }

  @Override
  public void setStorageValue(final UInt256 key, final UInt256 value) {
    storage.put(key, value);
  }

  @Override
  public void clearStorage() {
    storage.clear();
  }

  @Override
  public Map<UInt256, UInt256> getUpdatedStorage() {
    throw new UnsupportedOperationException("Not implemented");
  }
}
