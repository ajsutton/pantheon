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

import tech.pegasys.pantheon.ethereum.mainnet.account.AccountInit;

import java.util.Collection;

/**
 * An object that buffers updates made over a particular {@link WorldView}.
 *
 * <p>All changes made to this object, being it account creation/deletion or account modifications
 * through {@link MutableAccount}, are immediately reflected on this object (so for instance,
 * deleting an account and trying to get it afterwards will return {@code null}) but do not impact
 * whichever {@link WorldView} this is an updater for until the {@link #commit} method is called.
 */
public interface WorldUpdater extends MutableWorldView {

  /**
   * Creates a new account, or reset it (that is, act as if it was deleted and created anew) if it
   * already exists.
   *
   * @param address the address of the account to create (or reset).
   * @param defaultSetup the {@link AccountInit} to apply default values with.
   * @param currentBlockNumber the block number the account is first created in
   * @return the account {@code address}, which will have 0 for the nonce and balance and empty code
   *     and storage.
   */
  MutableAccount createAccount(Address address, AccountInit defaultSetup, long currentBlockNumber);

  /**
   * Retrieves the provided account if it exists, or create it if it doesn't.
   *
   * @param address the address of the account.
   * @param defaultSetup the {@link AccountInit} to apply default values with.
   * @param currentBlockNumber the block number the account is first created in
   * @return the account {@code address}. If that account exists, it is returned as if by {@link
   *     #getMutable(Address)}, otherwise, it is created and returned as if by {@link
   *     #createAccount(Address, AccountInit, long)} (and thus all his fields will be zero/empty).
   */
  default MutableAccount getOrCreate(
      final Address address, final AccountInit defaultSetup, final long currentBlockNumber) {
    final MutableAccount account = getMutable(address);
    return account == null ? createAccount(address, defaultSetup, currentBlockNumber) : account;
  }

  /**
   * Retrieves the provided account, returning a modifiable object (whose updates are accumulated by
   * this updater).
   *
   * @param address the address of the account.
   * @return the account {@code address} as modifiable object, or {@code null} if the account does
   *     not exist.
   */
  MutableAccount getMutable(Address address);

  /**
   * Deletes the provided account.
   *
   * @param address the address of the account to delete. If that account doesn't exists prior to
   *     this call, this is a no-op.
   */
  void deleteAccount(Address address);

  /**
   * Returns the accounts that have been touched within the scope of this updater.
   *
   * @return the accounts that have been touched within the scope of this updater
   */
  Collection<MutableAccount> getTouchedAccounts();

  /** Removes the changes that were made to this updater. */
  void revert();

  /**
   * Commits the changes made to this updater to the underlying {@link WorldView} this is an updater
   * of.
   */
  void commit();
}
