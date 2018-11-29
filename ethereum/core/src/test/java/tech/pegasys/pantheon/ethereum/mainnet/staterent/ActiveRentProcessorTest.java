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

import org.junit.Test;

public class ActiveRentProcessorTest {

  private static final Wei RENT_COST = Wei.fromGwei(2);
  private static final int RENT_ENABLED_BLOCK_NUMBER = 1000;

  private final MutableAccount account = new StubAccount();

  private final RentProcessor rentProcessor =
      new ActiveRentProcessor(RENT_COST, RENT_ENABLED_BLOCK_NUMBER);

  @Test
  public void shouldReportRentAsActive() {
    assertThat(rentProcessor.isRentCharged()).isTrue();
  }

  @Test
  public void shouldNotChargeRentWhenCurrentBlockNumberIsSameAsRentBlock() {
    final Wei initialBalance = Wei.fromGwei(1_000_000);
    account.setBalance(initialBalance);
    rentProcessor.chargeRent(account, RENT_ENABLED_BLOCK_NUMBER);

    assertThat(account.getBalance()).isEqualTo(initialBalance);
  }
}
