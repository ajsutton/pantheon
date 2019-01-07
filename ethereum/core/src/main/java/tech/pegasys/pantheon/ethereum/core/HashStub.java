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
package tech.pegasys.pantheon.ethereum.core;

import com.google.common.base.MoreObjects;

public class HashStub {

  private final Address address;
  private final Hash addressHash;
  private final Hash storageRoot;
  private final Hash codeHash;

  public HashStub(
      final Address address, final Hash addressHash, final Hash storageRoot, final Hash codeHash) {
    this.address = address;
    this.addressHash = addressHash;
    this.storageRoot = storageRoot;
    this.codeHash = codeHash;
  }

  public Address getAddress() {
    return address;
  }

  public Hash getAddressHash() {
    return addressHash;
  }

  public Hash getStorageRoot() {
    return storageRoot;
  }

  public Hash getCodeHash() {
    return codeHash;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("address", address)
        .add("addressHash", addressHash)
        .add("storageRoot", storageRoot)
        .add("codeHash", codeHash)
        .toString();
  }
}
