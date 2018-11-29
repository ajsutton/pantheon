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

import tech.pegasys.pantheon.ethereum.core.MutableAccount;
import tech.pegasys.pantheon.ethereum.core.Wei;

public class ActiveRentProcessor implements RentProcessor {

  private final Wei rentCost;
  private final long rentEnabledBlockNumber;

  public ActiveRentProcessor(final Wei rentCost, final long rentEnabledBlockNumber) {
    this.rentCost = rentCost;
    this.rentEnabledBlockNumber = rentEnabledBlockNumber;
  }

  @Override
  public void chargeRent(final MutableAccount account, final long currentBlockNumber) {}

  @Override
  public boolean isRentCharged() {
    return true;
  }
}
