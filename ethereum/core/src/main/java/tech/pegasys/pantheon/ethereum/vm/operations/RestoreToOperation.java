/*
 * Copyright 2019 ConsenSys AG.
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
import tech.pegasys.pantheon.ethereum.core.Hash;
import tech.pegasys.pantheon.ethereum.core.HashStub;
import tech.pegasys.pantheon.ethereum.core.MutableAccount;
import tech.pegasys.pantheon.ethereum.core.Wei;
import tech.pegasys.pantheon.ethereum.core.WorldUpdater;
import tech.pegasys.pantheon.ethereum.vm.AbstractOperation;
import tech.pegasys.pantheon.ethereum.vm.GasCalculator;
import tech.pegasys.pantheon.ethereum.vm.MessageFrame;

import java.math.BigInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RestoreToOperation extends AbstractOperation {

  private static final Logger LOG = LogManager.getLogger();

  public RestoreToOperation(final GasCalculator gasCalculator) {
    super(0x49, "RESTORETO", 2, 0, false, 1, gasCalculator);
  }

  @Override
  public Gas cost(final MessageFrame frame) {
    return Gas.ZERO; // Actual gas cost TBD
  }

  @Override
  public void execute(final MessageFrame frame) {
    final Address codeAddress = Address.wrap(frame.popStackItem());
    final Address address = Address.wrap(frame.popStackItem());

    final WorldUpdater worldState = frame.getWorldState();
    final Account account = worldState.get(frame.getRecipientAddress());
    final Wei inheritance = account != null ? account.getBalance() : Wei.ZERO;
    final Hash storageRoot = account.calculateStorageRoot();

    final Account codeSource = worldState.get(codeAddress);
    final Hash codeHash = codeSource != null ? codeSource.getCodeHash() : Hash.EMPTY;

    final HashStub evictedAccount = worldState.getHashStub(address);
    if (evictedAccount == null) {
      LOG.debug("Restore rejected because the target address is not a hash stub");
      return;
    }

    if (!codeHash.equals(evictedAccount.getCodeHash())) {
      LOG.debug(
          "Restore rejected because code hash {} does not match stub {}",
          codeHash,
          evictedAccount.getCodeHash());
      return;
    }

    if (!storageRoot.equals(evictedAccount.getStorageRoot())) {
      LOG.debug(
          "Restore rejected because storage root {} does not match stub {}",
          storageRoot,
          evictedAccount.getStorageRoot());
      return;
    }

    final MutableAccount restoredAccount =
        worldState.createAccount(
            address, frame.getAccountInit(), frame.getBlockHeader().getNumber());
    restoredAccount.setCode(codeSource.getCode());
    restoredAccount.copyStorageFrom(account);
    restoredAccount.setBalance(inheritance);
    restoredAccount.setRentBalance(account != null ? account.getRentBalance() : BigInteger.ZERO);
    frame.addSelfDestruct(account.getAddress());
  }
}
