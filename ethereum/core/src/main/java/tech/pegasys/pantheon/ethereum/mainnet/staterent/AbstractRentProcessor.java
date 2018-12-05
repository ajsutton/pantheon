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

import java.math.BigInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class AbstractRentProcessor implements RentProcessor {

  private static final Logger LOG = LogManager.getLogger();

  @Override
  public void chargeRent(final MutableAccount account, final long currentBlockNumber) {
    BigInteger newRentBalance = calculateNewRentBalance(account, currentBlockNumber);
    if (newRentBalance.signum() < 0) {
      final Wei rentStillOwing = Wei.of(newRentBalance.negate());
      final Wei repayment =
          account.getBalance().compareTo(rentStillOwing) <= 0
              ? account.getBalance()
              : rentStillOwing;
      newRentBalance = newRentBalance.add(repayment.asBigInteger());
      account.decrementBalance(repayment);
    }

    account.setRentBalance(newRentBalance);
    account.setRentBlock(currentBlockNumber);
  }

  private BigInteger calculateNewRentBalance(final Account account, final long currentBlockNumber) {
    final BigInteger rentDue = calculateRentDue(account, currentBlockNumber);
    LOG.debug(
        "Rent charge for {} calculated as {} wei from blocks {} to {}",
        rentDue,
        account.getAddress(),
        account.getRentBlock());
    return account.getRentBalance().subtract(rentDue);
  }

  protected abstract BigInteger calculateRentDue(Account account, long currentBlockNumber);

  @Override
  public boolean isRentEnabled() {
    return true;
  }

  @Override
  public boolean isEligibleForEviction(final Account account, final long currentBlockNumber) {
    final BigInteger newRentBalance = calculateNewRentBalance(account, currentBlockNumber);
    return newRentBalance.signum() < 0
        && account.getBalance().asBigInteger().compareTo(newRentBalance.abs()) < 0;
  }
}
