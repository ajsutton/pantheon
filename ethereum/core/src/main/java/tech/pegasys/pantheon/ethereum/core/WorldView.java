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
package tech.pegasys.pantheon.ethereum.core;

/** Generic interface for a view over the accounts of the world state. */
public interface WorldView {
  WorldView EMPTY =
      new WorldView() {
        @Override
        public Account get(final Address address) {
          return null;
        }

        @Override
        public HashStub getHashStub(final Address address) {
          return null;
        }
      };

  /**
   * Get an account provided it's address.
   *
   * @param address the address of the account to retrieve.
   * @return the {@link Account} corresponding to {@code address} or {@code null} if there is no
   *     such account or it has been evicted.
   */
  Account get(Address address);

  /**
   * Get the hash stub for an evicted account provided it's address.
   *
   * @param address the address of the account hash stub to retrieve.
   * @return the {@link HashStub} corresponding to {@code address} or {@code null} if the account at
   *     the specified address does not exist or is not evicted.
   */
  HashStub getHashStub(Address address);
}
