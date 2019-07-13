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
package tech.pegasys.pantheon.consensus.common.jsonrpc;

import tech.pegasys.pantheon.consensus.common.VoteTallyCache;
import tech.pegasys.pantheon.ethereum.core.Address;
import tech.pegasys.pantheon.ethereum.core.BlockHeader;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.JsonRpcRequest;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.parameters.JsonRpcParameter;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.queries.BlockchainQueries;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.response.JsonRpcError;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.response.JsonRpcErrorResponse;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.response.JsonRpcResponse;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.response.JsonRpcSuccessResponse;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.results.report.block.production.ProposerReportBlockProductionResult;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.results.report.block.production.SignerMetricsResult;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.results.report.block.production.ValidatorReportBlockProductionResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.LongStream;

public abstract class AbstractGetSignerMetricsMethod {

  private final BlockchainQueries blockchainQueries;
  private final VoteTallyCache voteTallyCache;
  private final JsonRpcParameter parameters;

  public AbstractGetSignerMetricsMethod(
      final BlockchainQueries blockchainQueries,
      final VoteTallyCache voteTallyCache,
      final JsonRpcParameter parameter) {
    this.blockchainQueries = blockchainQueries;
    this.voteTallyCache = voteTallyCache;
    this.parameters = parameter;
  }

  public JsonRpcResponse response(final JsonRpcRequest request) {

    final long startBlock = parameters.required(request.getParams(), 0, Long.class);
    final long endBlock = parameters.required(request.getParams(), 1, Long.class);

    if (isValidParameters(startBlock, endBlock)) {

      final Map<Address, ProposerReportBlockProductionResult> proposersMap = new HashMap<>();
      final Map<Address, ValidatorReportBlockProductionResult> validatorInLastBlockMap =
          new HashMap<>();

      // go through each block (startBlock is inclusive and endBlock is exclusive)
      LongStream.range(startBlock, endBlock)
          .boxed()
          .sorted(Collections.reverseOrder())
          .forEach(
              currentIndex -> {
                Optional<BlockHeader> blockHeaderByNumber =
                    blockchainQueries.getBlockHeaderByNumber(currentIndex);

                blockHeaderByNumber.ifPresent(
                    header -> {
                      if (validatorInLastBlockMap.isEmpty()) {
                        // Get all validators present in the last block of the range
                        voteTallyCache
                            .getVoteTallyAfterBlock(header)
                            .getValidators()
                            .forEach(
                                address ->
                                    validatorInLastBlockMap.put(
                                        address,
                                        new ValidatorReportBlockProductionResult(address)));
                      }

                      // Get the number of blocks from each proposer in a given block range.
                      getProposersOfBlock(header)
                          .forEach(
                              proposerAddress -> {
                                final ProposerReportBlockProductionResult proposer =
                                    proposersMap.get(proposerAddress);
                                if (proposer != null) {
                                  proposer.incrementeNbBlock();
                                } else {
                                  proposersMap.put(
                                      proposerAddress,
                                      new ProposerReportBlockProductionResult(proposerAddress));
                                }

                                // Add the block number of the last block proposed by each validator
                                // (if any within the
                                // given range)
                                final ValidatorReportBlockProductionResult validator =
                                    validatorInLastBlockMap.get(proposerAddress);
                                if (validator != null && !validator.isLastBlockAlreadyFound()) {
                                  validator.setLastBlockProposed(currentIndex);
                                }
                              });
                    });
              });

      return new JsonRpcSuccessResponse(
          request.getId(),
          new SignerMetricsResult(
              new ArrayList<>(proposersMap.values()),
              new ArrayList<>(validatorInLastBlockMap.values())));
    } else {
      return new JsonRpcErrorResponse(request.getId(), JsonRpcError.INVALID_PARAMS);
    }
  }

  private boolean isValidParameters(final long startBlock, final long endBlock) {
    return startBlock < endBlock;
  }

  protected abstract List<Address> getProposersOfBlock(final BlockHeader header);
}
