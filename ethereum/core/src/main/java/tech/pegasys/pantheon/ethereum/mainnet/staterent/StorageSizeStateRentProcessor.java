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
package tech.pegasys.pantheon.ethereum.mainnet.staterent;

import tech.pegasys.pantheon.ethereum.core.Account;
import tech.pegasys.pantheon.ethereum.core.MutableAccount;
import tech.pegasys.pantheon.ethereum.core.Wei;
import tech.pegasys.pantheon.ethereum.core.WorldUpdater;

import java.math.BigInteger;

public class StorageSizeStateRentProcessor extends AbstractRentProcessor {

  private final BigInteger rentCost;
  private final long rentEnabledBlockNumber;

  public StorageSizeStateRentProcessor(final Wei rentCost, final long rentEnabledBlockNumber) {
    this.rentCost = rentCost.asBigInteger();
    this.rentEnabledBlockNumber = rentEnabledBlockNumber;
  }

  @Override
  public void chargeRent(final MutableAccount account, final long currentBlockNumber) {
    if (account.getStorageSize() == null) {
      account.setStorageSize(Account.EMPTY_ACCOUNT_STORAGE_SIZE);
    }
    super.chargeRent(account, currentBlockNumber);
  }

  @Override
  protected void evictAccount(final WorldUpdater worldState, final MutableAccount account) {
    if (account.hasCode()) {
      worldState.evictAccount(account.getAddress());
    } else {
      worldState.deleteAccount(account.getAddress());
    }
  }

  @Override
  protected BigInteger calculateRentDue(final Account account, final long currentBlockNumber) {
    final long rentBlock =
        account.getRentBlock() == Account.NO_RENT_BLOCK
            ? rentEnabledBlockNumber
            : account.getRentBlock();
    final BigInteger storageSize =
        account.getOriginalStorageSize() != null
            ? account.getOriginalStorageSize()
            : Account.EMPTY_ACCOUNT_STORAGE_SIZE;
    return rentCost
        .multiply(storageSize)
        .multiply(BigInteger.valueOf(currentBlockNumber - rentBlock));
  }
}
