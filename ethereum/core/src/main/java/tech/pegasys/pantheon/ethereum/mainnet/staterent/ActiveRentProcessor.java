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

import static tech.pegasys.pantheon.util.bytes.BytesValues.asUnsignedBigInteger;

import tech.pegasys.pantheon.ethereum.core.Account;
import tech.pegasys.pantheon.ethereum.core.MutableAccount;
import tech.pegasys.pantheon.ethereum.core.Wei;

import java.math.BigInteger;

public class ActiveRentProcessor implements RentProcessor {

  private final Wei rentCost;
  private final long rentEnabledBlockNumber;

  public ActiveRentProcessor(final Wei rentCost, final long rentEnabledBlockNumber) {
    this.rentCost = rentCost;
    this.rentEnabledBlockNumber = rentEnabledBlockNumber;
  }

  @Override
  public void chargeRent(final MutableAccount account, final long currentBlockNumber) {
    if (account.getRentBlock() == Account.NEW_ACCOUNT_RENT_BLOCK) {
      account.setRentBlock(currentBlockNumber);
      return;
    }
    final long rentBlock = account.getRentBlock() == Account.NO_RENT_BLOCK ? rentEnabledBlockNumber
        : account.getRentBlock();
    final Wei rentDue = rentCost.times(currentBlockNumber - rentBlock);
    BigInteger newRentBalance = account.getRentBalance()
        .subtract(asUnsignedBigInteger(rentDue.getBytes()));
    if (newRentBalance.signum() < 0) {
      final Wei rentStillOwing = Wei.of(newRentBalance.negate());
      final Wei repayment =
          account.getBalance().compareTo(rentStillOwing) <= 0 ? account.getBalance()
              : rentStillOwing;
      newRentBalance = newRentBalance.add(asUnsignedBigInteger(repayment.getBytes()));
      account.decrementBalance(repayment);
    }

    account.setRentBalance(newRentBalance);
    account.setRentBlock(currentBlockNumber);
  }

  @Override
  public boolean isRentCharged() {
    return true;
  }
}
