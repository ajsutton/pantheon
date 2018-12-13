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

import tech.pegasys.pantheon.ethereum.core.Address;
import tech.pegasys.pantheon.ethereum.core.Gas;
import tech.pegasys.pantheon.ethereum.core.MutableAccount;
import tech.pegasys.pantheon.ethereum.core.Wei;
import tech.pegasys.pantheon.ethereum.core.WorldUpdater;
import tech.pegasys.pantheon.ethereum.vm.AbstractOperation;
import tech.pegasys.pantheon.ethereum.vm.GasCalculator;
import tech.pegasys.pantheon.ethereum.vm.MessageFrame;

import java.math.BigInteger;

public class PayRentOperation extends AbstractOperation {

  public PayRentOperation(final GasCalculator gasCalculator) {
    super(0x4F, "PAYRENT", 2, 0, false, 1, gasCalculator);
  }

  @Override
  public Gas cost(final MessageFrame frame) {
    return gasCalculator().getBaseTierGasCost(); // Actual gas cost TBD.
  }

  @Override
  public void execute(final MessageFrame frame) {
    final Address rentRecipientAddress = Address.wrap(frame.popStackItem());
    final BigInteger rentPayment = frame.popStackItem().asUInt256().asBigInteger();
    final long blockNumber = frame.getBlockHeader().getNumber();
    final WorldUpdater worldState = frame.getWorldState();

    final MutableAccount payer =
        worldState.getOrCreate(frame.getRecipientAddress(), frame.getAccountInit(), blockNumber);
    payer.decrementBalance(Wei.of(rentPayment));

    final MutableAccount rentRecipient =
        worldState.getOrCreate(rentRecipientAddress, frame.getAccountInit(), blockNumber);
    rentRecipient.setRentBalance(rentRecipient.getRentBalance().add(rentPayment));
  }
}
