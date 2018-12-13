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
package tech.pegasys.pantheon.ethereum.vm.operations;

import tech.pegasys.pantheon.ethereum.core.Account;
import tech.pegasys.pantheon.ethereum.core.Address;
import tech.pegasys.pantheon.ethereum.core.Gas;
import tech.pegasys.pantheon.ethereum.vm.AbstractOperation;
import tech.pegasys.pantheon.ethereum.vm.GasCalculator;
import tech.pegasys.pantheon.ethereum.vm.MessageFrame;
import tech.pegasys.pantheon.util.bytes.Bytes32;
import tech.pegasys.pantheon.util.uint.Int256;

public class SSizeOperation extends AbstractOperation {

  public SSizeOperation(final GasCalculator gasCalculator) {
    super(0x48, "SSIZE", 1, 1, false, 1, gasCalculator);
  }

  @Override
  public Gas cost(final MessageFrame frame) {
    // Actual gas cost TBD but "like EXTCODEHASH"
    return gasCalculator().extCodeHashOperationGasCost();
  }

  @Override
  public void execute(final MessageFrame frame) {
    final Address address = Address.wrap(frame.popStackItem());
    final Account account = frame.getWorldState().get(address);

    if (account == null) {
      frame.pushStackItem(Bytes32.ZERO);
    } else {
      frame.pushStackItem(Int256.of(account.getStorageSize()).getBytes());
    }
  }
}
