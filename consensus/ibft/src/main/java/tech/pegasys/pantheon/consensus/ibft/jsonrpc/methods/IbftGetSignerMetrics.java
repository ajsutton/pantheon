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
package tech.pegasys.pantheon.consensus.ibft.jsonrpc.methods;

import tech.pegasys.pantheon.consensus.common.VoteTallyCache;
import tech.pegasys.pantheon.consensus.common.jsonrpc.AbstractGetSignerMetricsMethod;
import tech.pegasys.pantheon.consensus.ibft.IbftHelpers;
import tech.pegasys.pantheon.ethereum.core.Address;
import tech.pegasys.pantheon.ethereum.core.BlockHeader;
import tech.pegasys.pantheon.ethereum.jsonrpc.RpcMethod;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.methods.JsonRpcMethod;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.parameters.JsonRpcParameter;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.queries.BlockchainQueries;

import java.util.List;

public class IbftGetSignerMetrics extends AbstractGetSignerMetricsMethod implements JsonRpcMethod {

  public IbftGetSignerMetrics(
      final BlockchainQueries blockchainQueries,
      final VoteTallyCache voteTallyCache,
      final JsonRpcParameter parameter) {
    super(blockchainQueries, voteTallyCache, parameter);
  }

  @Override
  public String getName() {
    return RpcMethod.IBFT_GET_SIGNER_METRICS.getMethodName();
  }

  @Override
  protected List<Address> getProposersOfBlock(final BlockHeader header) {
    return IbftHelpers.getCommitterOfBlock(header);
  }
}
