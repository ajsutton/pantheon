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
package tech.pegasys.pantheon.consensus.clique.jsonrpc.methods;

import tech.pegasys.pantheon.consensus.clique.CliqueHelpers;
import tech.pegasys.pantheon.consensus.clique.jsonrpc.response.GetReportValidatorBlockProductionResponse;
import tech.pegasys.pantheon.consensus.clique.jsonrpc.response.ProposerReportBlockProduction;
import tech.pegasys.pantheon.consensus.clique.jsonrpc.response.ValidatorReportBlockProduction;
import tech.pegasys.pantheon.consensus.common.VoteTallyCache;
import tech.pegasys.pantheon.ethereum.core.Address;
import tech.pegasys.pantheon.ethereum.core.BlockHeader;
import tech.pegasys.pantheon.ethereum.jsonrpc.RpcMethod;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.JsonRpcRequest;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.methods.JsonRpcMethod;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.parameters.JsonRpcParameter;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.queries.BlockchainQueries;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.response.JsonRpcError;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.response.JsonRpcErrorResponse;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.response.JsonRpcResponse;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.response.JsonRpcSuccessResponse;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class CliqueGetReportValidatorBlockProduction implements JsonRpcMethod {
    private final BlockchainQueries blockchainQueries;
    private final VoteTallyCache voteTallyCache;
    private final JsonRpcParameter parameters;

    public CliqueGetReportValidatorBlockProduction(
            final BlockchainQueries blockchainQueries,
            final VoteTallyCache voteTallyCache,
            final JsonRpcParameter parameter) {
        this.blockchainQueries = blockchainQueries;
        this.voteTallyCache = voteTallyCache;
        this.parameters = parameter;
    }

    @Override
    public String getName() {
        return RpcMethod.CLIQUE_GET_SIGNERS_AT_HASH.getMethodName();
    }

    @Override
    public JsonRpcResponse response(final JsonRpcRequest request) {

        final long startBlock = parameters.required(request.getParams(), 0, Long.class);
        final long endBlock = parameters.required(request.getParams(), 1, Long.class);

        if (isValidParameter(startBlock, endBlock)) {

            final Map<Address, ProposerReportBlockProduction> proposersMap = new HashMap<>();
            final Map<Address, ValidatorReportBlockProduction> validatorInLastBlockMap = new HashMap<>();
            final long lastBlockIndex = endBlock - 1;

            for (long currentBlockIndex = lastBlockIndex; currentBlockIndex >= startBlock; currentBlockIndex -= 1) {
                Optional<BlockHeader> blockHeaderByNumber = blockchainQueries.getBlockHeaderByNumber(currentBlockIndex);
                if (blockHeaderByNumber.isPresent()) {
                    final BlockHeader blockHeader = blockHeaderByNumber.get();
                    // Get all validators present in the last block of the range
                    if (validatorInLastBlockMap.isEmpty()) {
                        voteTallyCache
                                .getVoteTallyAfterBlock(blockHeader)
                                .getValidators().forEach(address -> {
                            validatorInLastBlockMap.put(address, new ValidatorReportBlockProduction(address));
                        });
                    }
                    // Get the number of blocks from each proposer in a given block range.
                    final Address proposerOfBlock = CliqueHelpers.getProposerOfBlock(blockHeader);
                    final ProposerReportBlockProduction proposer = proposersMap.get(proposerOfBlock);
                    if (proposer != null) {
                        proposer.incrementeNbBlock();
                    } else {
                        proposersMap.put(proposerOfBlock, new ProposerReportBlockProduction(proposerOfBlock));
                    }
                    //Add the block number of the last block proposed by each validator (if any within the given range)
                    final ValidatorReportBlockProduction validator = validatorInLastBlockMap.get(proposerOfBlock);
                    if (validator != null) {
                        validator.setLastBlock(currentBlockIndex);
                    }
                    new JsonRpcSuccessResponse(request.getId(),
                            new GetReportValidatorBlockProductionResponse(
                                    new ArrayList<>(proposersMap.values()),
                                    new ArrayList<>(validatorInLastBlockMap.values())
                            )
                    );
                }
            }
            return new JsonRpcErrorResponse(request.getId(), JsonRpcError.INTERNAL_ERROR);
        } else {
            return new JsonRpcErrorResponse(request.getId(), JsonRpcError.INVALID_PARAMS);
        }
    }

    private boolean isValidParameter(long startBlock, long endBlock) {
        return startBlock < endBlock;
    }
}
