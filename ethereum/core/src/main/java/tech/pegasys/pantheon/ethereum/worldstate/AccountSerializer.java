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
import tech.pegasys.pantheon.ethereum.core.HashStub;
import tech.pegasys.pantheon.ethereum.core.Wei;
import tech.pegasys.pantheon.ethereum.rlp.RLP;
import tech.pegasys.pantheon.ethereum.rlp.RLPException;
import tech.pegasys.pantheon.ethereum.rlp.RLPInput;
import tech.pegasys.pantheon.util.bytes.BytesValue;

import java.math.BigInteger;
import java.util.Optional;

public class AccountSerializer<T> {
  private final AccountStateConstructor<T> accountStateConstructor;

  public AccountSerializer(final AccountStateConstructor<T> accountStateConstructor) {
    this.accountStateConstructor = accountStateConstructor;
  }

  public Optional<T> deserializeAccount(
      final Address address, final Hash addressHash, final BytesValue encoded) throws RLPException {
    final RLPInput in = RLP.input(encoded);
    in.enterList();

    if (!in.isLongScalar()) {
      // We have a hash stub, not a full account.
      return Optional.empty();
    }
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

    final BigInteger storageSize;
    if (!in.isEndOfCurrentList()) {
      storageSize = new BigInteger(in.readBytesValue().getArrayUnsafe());
    } else {
      storageSize = null;
    }

    in.leaveList();

    return Optional.of(
        accountStateConstructor.create(
            address,
            addressHash,
            nonce,
            balance,
            rentBalance,
            rentBlock,
            storageSize,
            storageRoot,
            codeHash));
  }

  public BytesValue serializeAccount(
      final Account account, final Hash codeHash, final Hash storageRoot) {
    return RLP.encode(
        out -> {
          out.startList();

          out.writeLongScalar(account.getNonce());
          out.writeUInt256Scalar(account.getBalance());
          out.writeBytesValue(storageRoot);
          out.writeBytesValue(codeHash);
          final long rentBlock = account.getRentBlock();
          final BigInteger storageSize = account.getStorageSize();
          final boolean hasRentBlock = rentBlock != Account.NO_RENT_BLOCK;
          final boolean hasStorageSize = storageSize != null;
          if (hasRentBlock || hasStorageSize) {
            out.writeBytesValue(BytesValue.wrap(account.getRentBalance().toByteArray()));
            out.writeLongScalar(hasRentBlock ? rentBlock : 0);
          }

          if (hasStorageSize) {
            out.writeBytesValue(BytesValue.wrap(storageSize.toByteArray()));
          }

          out.endList();
        });
  }

  public BytesValue serializeHashStub(final Hash codeHash, final Hash storageRoot) {
    return RLP.encode(
        out -> {
          out.startList();
          out.writeBytesValue(codeHash);
          out.writeBytesValue(storageRoot);
          out.endList();
        });
  }

  public Optional<HashStub> deserializeHashStub(
      final Address address, final Hash addressHash, final BytesValue encoded) throws RLPException {
    final RLPInput in = RLP.input(encoded);
    in.enterList();
    if (in.isLongScalar()) {
      // This is an actual account, not a hash stub.
      return Optional.empty();
    }
    final Hash storageRoot = Hash.wrap(in.readBytes32());
    final Hash codeHash = Hash.wrap(in.readBytes32());
    in.leaveList();
    return Optional.of(new HashStub(address, addressHash, storageRoot, codeHash));
  }

  public interface AccountStateConstructor<T> {
    T create(
        final Address address,
        final Hash addressHash,
        final long nonce,
        final Wei balance,
        final BigInteger rentBalance,
        final long rentBlock,
        final BigInteger storageSize,
        final Hash storageRoot,
        final Hash codeHash);
  }
}
