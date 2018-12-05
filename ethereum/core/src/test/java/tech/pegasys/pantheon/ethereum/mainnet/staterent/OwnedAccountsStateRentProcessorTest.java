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

import static org.assertj.core.api.Assertions.assertThat;

import tech.pegasys.pantheon.ethereum.core.MutableAccount;
import tech.pegasys.pantheon.ethereum.core.StubAccount;
import tech.pegasys.pantheon.ethereum.core.Wei;
import tech.pegasys.pantheon.util.bytes.BytesValue;

import java.math.BigInteger;

import org.junit.Test;

public class OwnedAccountsStateRentProcessorTest {

  private static final Wei RENT_COST = Wei.of(2);
  private static final int RENT_ENABLED_BLOCK_NUMBER = 1000;

  private final MutableAccount account = new StubAccount();

  private final RentProcessor rentProcessor =
      new OwnedAccountsStateRentProcessor(RENT_COST, RENT_ENABLED_BLOCK_NUMBER);

  @Test
  public void shouldReportRentAsActive() {
    assertThat(rentProcessor.isRentEnabled()).isTrue();
  }

  @Test
  public void shouldNotChargeRentWhenAccountHasCode() {
    final Wei initialBalance = Wei.of(1_000_000);
    account.setBalance(initialBalance);
    account.setRentBlock(0);
    account.setCode(BytesValue.of(1, 2, 3));

    rentProcessor.chargeRent(account, RENT_ENABLED_BLOCK_NUMBER);

    assertThat(account.getBalance()).isEqualTo(initialBalance);
    assertThat(account.getRentBlock()).isEqualTo(0);
    assertThat(account.getRentBalance()).isEqualTo(BigInteger.ZERO);
  }

  @Test
  public void shouldNotChargeRentWhenCurrentBlockNumberIsSameAsRentBlock() {
    final Wei initialBalance = Wei.of(1_000_000);
    account.setBalance(initialBalance);
    rentProcessor.chargeRent(account, RENT_ENABLED_BLOCK_NUMBER);

    assertThat(account.getBalance()).isEqualTo(initialBalance);
    assertThat(account.getRentBlock()).isEqualTo(RENT_ENABLED_BLOCK_NUMBER);
    assertThat(account.getRentBalance()).isEqualTo(BigInteger.ZERO);
  }

  @Test
  public void shouldChargeRentToRentBalanceWhenRentBalanceIsSufficient() {
    final Wei initialBalance = Wei.of(10_000);
    account.setBalance(initialBalance);
    account.setRentBalance(BigInteger.valueOf(50));
    account.setRentBlock(RENT_ENABLED_BLOCK_NUMBER);

    rentProcessor.chargeRent(account, RENT_ENABLED_BLOCK_NUMBER + 10);

    assertThat(account.getRentBalance()).isEqualTo(BigInteger.valueOf(30));
    assertThat(account.getRentBlock()).isEqualTo(RENT_ENABLED_BLOCK_NUMBER + 10);
    assertThat(account.getBalance()).isEqualTo(initialBalance);
  }

  @Test
  public void shouldReduceBalanceByRentDueWhenRentBalanceIsZero() {
    final Wei initialBalance = Wei.of(10_000);
    account.setBalance(initialBalance);
    account.setRentBalance(BigInteger.ZERO);
    account.setRentBlock(RENT_ENABLED_BLOCK_NUMBER);

    rentProcessor.chargeRent(account, RENT_ENABLED_BLOCK_NUMBER + 10);

    assertThat(account.getRentBalance()).isEqualTo(BigInteger.ZERO);
    assertThat(account.getRentBlock()).isEqualTo(RENT_ENABLED_BLOCK_NUMBER + 10);
    assertThat(account.getBalance()).isEqualTo(Wei.of(10_000 - 20));
  }
}
