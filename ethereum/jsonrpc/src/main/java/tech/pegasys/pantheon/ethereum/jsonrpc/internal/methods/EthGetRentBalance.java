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
package tech.pegasys.pantheon.ethereum.jsonrpc.internal.methods;

import tech.pegasys.pantheon.ethereum.core.Address;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.JsonRpcRequest;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.parameters.BlockParameter;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.parameters.JsonRpcParameter;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.queries.BlockchainQueries;

import java.math.BigInteger;

import com.fasterxml.jackson.annotation.JsonGetter;

public class EthGetRentBalance extends AbstractBlockParameterMethod {

  public EthGetRentBalance(final BlockchainQueries blockchain, final JsonRpcParameter parameters) {
    super(blockchain, parameters);
  }

  @Override
  public String getName() {
    return "eth_getRentBalance";
  }

  @Override
  protected BlockParameter blockParameter(final JsonRpcRequest request) {
    return parameters().required(request.getParams(), 1, BlockParameter.class);
  }

  @Override
  protected RentInfo resultByBlockNumber(final JsonRpcRequest request, final long blockNumber) {
    final Address address = parameters().required(request.getParams(), 0, Address.class);
    return blockchainQueries()
        .getAccount(address, blockNumber)
        .map(account -> new RentInfo(account.getRentBalance(), account.getStorageSize()))
        .orElse(null);
  }

  private static class RentInfo {
    private final String rentBalance;
    private final String storageSize;

    private RentInfo(final BigInteger rentBalance, final BigInteger storageSize) {
      this.rentBalance = rentBalance.toString();
      this.storageSize = storageSize != null ? storageSize.toString() : null;
    }

    @JsonGetter(value = "rentBalance")
    public String getRentBalance() {
      return rentBalance;
    }

    @JsonGetter(value = "storageSize")
    public String getStorageSize() {
      return storageSize;
    }
  }
}
