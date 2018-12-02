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
package tech.pegasys.pantheon.ethereum.worldstate;

import tech.pegasys.pantheon.ethereum.core.Account;
import tech.pegasys.pantheon.ethereum.core.Address;
import tech.pegasys.pantheon.ethereum.core.Hash;
import tech.pegasys.pantheon.ethereum.core.Wei;
import tech.pegasys.pantheon.ethereum.rlp.RLP;
import tech.pegasys.pantheon.ethereum.rlp.RLPException;
import tech.pegasys.pantheon.ethereum.rlp.RLPInput;
import tech.pegasys.pantheon.util.bytes.BytesValue;

import java.math.BigInteger;

public class AccountSerializer<T> {
  private final AccountStateConstructor<T> accountStateConstructor;

  public AccountSerializer(final AccountStateConstructor<T> accountStateConstructor) {
    this.accountStateConstructor = accountStateConstructor;
  }

  public T deserializeAccount(
      final Address address, final Hash addressHash, final BytesValue encoded) throws RLPException {
    final RLPInput in = RLP.input(encoded);
    in.enterList();

    final long nonce = in.readLongScalar();
    final Wei balance = in.readUInt256Scalar(Wei::wrap);
    final Hash storageRoot = Hash.wrap(in.readBytes32());
    final Hash codeHash = Hash.wrap(in.readBytes32());

    final long rentBlock;
    final BigInteger rentBalance;
    if (!in.isEndOfCurrentList()) {
      rentBalance = new BigInteger(in.readBytesValue().getArrayUnsafe());
      rentBlock = in.readLongScalar();
    } else {
      rentBalance = BigInteger.ZERO;
      rentBlock = Account.NO_RENT_BLOCK;
    }

    in.leaveList();

    return accountStateConstructor.create(
        address, addressHash, nonce, balance, rentBalance, rentBlock, storageRoot, codeHash);
  }

  public BytesValue serializeAccount(
      final long nonce,
      final Wei balance,
      final Hash codeHash,
      final Hash storageRoot,
      final BigInteger rentBalance,
      final long rentBlock) {
    return RLP.encode(
        out -> {
          out.startList();

          out.writeLongScalar(nonce);
          out.writeUInt256Scalar(balance);
          out.writeBytesValue(storageRoot);
          out.writeBytesValue(codeHash);
          if (rentBlock != Account.NO_RENT_BLOCK && rentBlock != Account.NEW_ACCOUNT_RENT_BLOCK) {
            out.writeBytesValue(BytesValue.wrap(rentBalance.toByteArray()));
            out.writeLongScalar(rentBlock);
          }

          out.endList();
        });
  }

  public interface AccountStateConstructor<T> {
    T create(
        final Address address,
        final Hash addressHash,
        final long nonce,
        final Wei balance,
        final BigInteger rentBalance,
        final long rentBlock,
        final Hash storageRoot,
        final Hash codeHash);
  }
}
